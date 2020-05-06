package com.real.world.http4s.http

import org.http4s.dsl.Http4sDsl
import org.http4s.{ HttpRoutes, Request, Response }

import cats.data.{ Kleisli, OptionT }
import cats.implicits._
import cats.{ ApplicativeError, MonadError }

import com.real.world.http4s.AppError

// ToDo this will be moved to routes level
class BaseHttpErrorHandler[F[_]](implicit M: MonadError[F, Throwable]) extends Http4sDsl[F] {

  private val handler: (Request[F], Throwable) => F[Response[F]] = {
    case (_, e: AppError) =>
      e.toHttpResponse()
    case (_, _) =>
      InternalServerError("Internal Server Error")
  }

  def handle(service: HttpRoutes[F]): HttpRoutes[F] =
    ServiceHttpErrorHandler(service)(handler)
}

object ServiceHttpErrorHandler {
  def apply[F[_], E, U](service: HttpRoutes[F])(handler: (Request[F], E) => F[Response[F]])(implicit ev: ApplicativeError[F, E]): HttpRoutes[F] =
    Kleisli { req: Request[F] =>
      OptionT {
        service(req).value.handleErrorWith(e => handler(req, e).map(Option(_)))
      }
    }
}
