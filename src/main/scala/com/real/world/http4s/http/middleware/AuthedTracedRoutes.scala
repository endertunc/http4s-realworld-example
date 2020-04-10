package com.real.world.http4s.http.middleware

import org.http4s._
import cats.data.{ Kleisli, OptionT }
import cats.effect.Sync
import cats.implicits._
import cats.{ Applicative, Defer, Functor }
import com.colisweb.tracing.core.{ TracingContext, TracingContextBuilder }
import io.opentracing.{ Span, Tracer }
import AuthedTracedRoutes.{ AuthedTraceContext, AuthedTracedContextRequest, AuthedTracedRoutes }
import com.real.world.http4s.model.UserId

object AuthedTracedMiddleware {

  def apply[F[_]: Sync: Functor](
      routes: AuthedTracedRoutes[F]
  )(implicit builder: TracingContextBuilder[F]): AuthedRoutes[UserId, F] =
    Kleisli[OptionT[F, *], AuthedRequest[F, UserId], Response[F]] { authedReq =>
      val req           = authedReq.req
      val operationName = "http4s-request"
      val tags = Map(
        "http_method" -> req.method.name,
        "request_url" -> req.uri.path
      )
      def transformResponse(context: TracingContext[F]): F[Option[Response[F]]] = {
        val tracedRequest: AuthedTracedContextRequest[F] =
          new ContextRequest[F, AuthedTraceContext[F]](new AuthedTraceContext[F](authedReq.context, context), req)
        val responseOptionWithTags = routes.run(tracedRequest).semiflatMap { response =>
          val traceTags = Map("http_status" -> response.status.code.toString) ++ tags
          context.addTags(traceTags).map(_ => response)
        }
        responseOptionWithTags.value
      }
      OptionT(builder.build(operationName, tags) use transformResponse)
    }

}

object AuthedTracedRoutes {

  case class AuthedTraceContext[F[_]](userId: UserId, tracingContext: TracingContext[F])
  type AuthedTracedContextRequest[F[_]] = ContextRequest[F, AuthedTraceContext[F]]
  type AuthedTracedRoutes[F[_]]         = Kleisli[OptionT[F, *], AuthedTracedContextRequest[F], Response[F]]

  def apply[T, F[_]](run: AuthedTracedContextRequest[F] => OptionT[F, Response[F]])(implicit F: Defer[F]): AuthedTracedRoutes[F] =
    Kleisli(req => OptionT(F.defer(run(req).value)))

  def of[F[_]](pf: PartialFunction[AuthedTracedContextRequest[F], F[Response[F]]])(implicit F: Defer[F], FA: Applicative[F]): AuthedTracedRoutes[F] =
    Kleisli(req => OptionT(F.defer(pf.lift(req).sequence)))

  object using {
    def unapply[F[_], T <: Tracer, S <: Span](tr: AuthedTracedContextRequest[F]): Option[(Request[F], AuthedTraceContext[F])] =
      Some(tr.req -> tr.context)
  }

}
