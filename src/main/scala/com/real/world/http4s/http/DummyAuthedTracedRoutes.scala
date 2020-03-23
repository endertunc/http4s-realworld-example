package com.real.world.http4s.http

import org.http4s.{ AuthedRequest, _ }

import cats.data.{ Kleisli, OptionT }
import cats.effect.Sync
import cats.implicits._

import com.colisweb.tracing.core.{ TracingContext, TracingContextBuilder }

import io.opentracing.{ Span, Tracer }

import com.real.world.http4s.model.UserId

case class AuthedTraceRequest[F[_]](authedRequest: AuthedRequest[F, UserId], tracingContext: TracingContext[F])

object DummyAuthedTracedRoutes {

  def apply[F[_]: Sync](
      pf: PartialFunction[AuthedTraceRequest[F], F[Response[F]]]
  )(implicit builder: TracingContextBuilder[F]): Kleisli[OptionT[F, ?], AuthedRequest[F, UserId], Response[F]] = {
    val tracedRoutes =
      Kleisli[OptionT[F, *], AuthedTraceRequest[F], Response[F]] { req =>
        pf.andThen(OptionT.liftF(_))
          .applyOrElse(req, Function.const(OptionT.none))
      }
    wrapHttpRoutes(tracedRoutes, builder)
  }

  def wrapHttpRoutes[F[_]: Sync](
      routes: Kleisli[OptionT[F, *], AuthedTraceRequest[F], Response[F]],
      builder: TracingContextBuilder[F]
  ): Kleisli[OptionT[F, *], AuthedRequest[F, UserId], Response[F]] =
    Kleisli[OptionT[F, *], AuthedRequest[F, UserId], Response[F]] { authedReq =>
      val req           = authedReq.req
      val operationName = "http4s-request"
      val tags = Map(
        "http_method" -> req.method.name,
        "request_url" -> req.uri.path
      )

      def transformResponse(context: TracingContext[F]): F[Option[Response[F]]] = {
        val tracedRequest = AuthedTraceRequest[F](authedReq, context)
        val responseOptionWithTags = routes.run(tracedRequest).semiflatMap { response =>
          val traceTags = Map("http_status" -> response.status.code.toString) ++ tags
          context.addTags(traceTags).map(_ => response)
        }
        responseOptionWithTags.value
      }

      OptionT(builder.build(operationName, tags) use transformResponse)

    }

  object using {
    def unapply[F[_], T <: Tracer, S <: Span](tr: AuthedTraceRequest[F]): Option[(AuthedRequest[F, UserId], TracingContext[F])] =
      Some(tr.authedRequest -> tr.tracingContext)
  }

}
