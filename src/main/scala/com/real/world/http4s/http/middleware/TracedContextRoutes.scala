package com.real.world.http4s.http.middleware

import org.http4s._

import cats.data.{ Kleisli, OptionT }
import cats.effect.Sync
import cats.implicits._
import cats.{ Applicative, Defer, Functor }

import com.colisweb.tracing.core.{ TracingContext, TracingContextBuilder }

import io.opentracing.{ Span, Tracer }

import TracedContextRoutes.{ TracedContext, TracedContextRequest, TracedContextRoutes }

object TracedContextMiddleware {

  def apply[F[_]: Sync: Functor, C](
      routes: TracedContextRoutes[F, C]
  )(implicit builder: TracingContextBuilder[F]): ContextRoutes[C, F] =
    Kleisli[OptionT[F, *], ContextRequest[F, C], Response[F]] { authedReq =>
      val req           = authedReq.req
      val operationName = "http4s-incoming-request"
      val tags = Map(
        "http_method" -> req.method.name,
        "request_url" -> req.uri.path
      )
      def transformResponse(context: TracingContext[F]): F[Option[Response[F]]] = {
        val tracedRequest: TracedContextRequest[F, C] =
          new ContextRequest[F, TracedContext[F, C]](new TracedContext[F, C](authedReq.context, context), req)
        val responseOptionWithTags = routes.run(tracedRequest).semiflatMap { response =>
          val traceTags = Map("http_status" -> response.status.code.toString) ++ tags
          context.addTags(traceTags).map(_ => response)
        }
        responseOptionWithTags.value
      }
      OptionT(builder.build(operationName, tags) use transformResponse)
    }

}

object TracedContextRoutes {

  case class TracedContext[F[_], T](context: T, tracingContext: TracingContext[F])
  type TracedContextRequest[F[_], T] = ContextRequest[F, TracedContext[F, T]]
  type TracedContextRoutes[F[_], T]  = Kleisli[OptionT[F, *], TracedContextRequest[F, T], Response[F]]

  def apply[T, F[_]](run: TracedContextRequest[F, T] => OptionT[F, Response[F]])(implicit F: Defer[F]): TracedContextRoutes[F, T] =
    Kleisli(req => OptionT(F.defer(run(req).value)))

  def of[F[_], T](
      pf: PartialFunction[TracedContextRequest[F, T], F[Response[F]]]
  )(implicit F: Defer[F], FA: Applicative[F]): TracedContextRoutes[F, T] =
    Kleisli(req => OptionT(F.defer(pf.lift(req).sequence)))

  // scalastyle:off object.name
  object using {
    def unapply[F[_], T <: Tracer, S <: Span, C](tr: TracedContextRequest[F, C]): Option[(Request[F], TracedContext[F, C])] =
      Some(tr.req -> tr.context)
  }
  // scalastyle:on object.name

}
