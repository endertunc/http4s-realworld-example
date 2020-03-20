//package com.real.world.http4s.http.error
//
//import com.real.world.http4s.AppError
//import com.real.world.http4s.AppError.{ ExceptionWrapper, UserNotFound }
//
//import cats.Monad
//
//import org.http4s.{ HttpVersion, Response }
//import org.http4s.dsl.Http4sDsl
//
//class HttpErrorHandler[F[_]: Monad] extends Http4sDsl[F] {
//
//  val handle: AppError => F[Response[F]] = { err: AppError =>
//    err.toHttpResponse(HttpVersion.`HTTP/2.0`)
//  }
//
//}
