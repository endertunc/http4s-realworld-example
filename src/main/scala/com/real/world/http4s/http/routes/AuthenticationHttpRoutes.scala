package com.real.world.http4s.http.routes

import org.http4s._

import cats.effect.Async
import cats.implicits._

import com.colisweb.tracing.core.TracingContextBuilder
import com.colisweb.tracing.http.server.TracedHttpRoutes
import com.colisweb.tracing.http.server.TracedHttpRoutes.using

import com.real.world.http4s.http.BaseHttp4s
import com.real.world.http4s.model.user.{ RegisterUser, RegisterUserWrapper, UserLoginWrapper }
import com.real.world.http4s.authentication.JwtAuthenticator
import com.real.world.http4s.service.UserService

import io.chrisdavenport.log4cats.Logger

class AuthenticationHttpRoutes[F[_]: Async: Logger: TracingContextBuilder]()(
    implicit userService: UserService[F],
    jwtAuthenticator: JwtAuthenticator[F]
) extends BaseHttp4s[F] {

  val routes: HttpRoutes[F] = TracedHttpRoutes[F] {
    case (req @ POST -> Root) using traceContext =>
      req.decode[RegisterUserWrapper] { userRegisterWrapper =>
        val userRegisterRequestIn: RegisterUser = userRegisterWrapper.user
        for {
          user                   <- userService.registerUser(userRegisterRequestIn)
          userResponseOutWrapper <- user.toUserResponseOutWrapper
          response               <- Ok(userResponseOutWrapper)
        } yield response
      }

    case (req @ POST -> Root / "login") using traceContext =>
      req.decode[UserLoginWrapper] { userLoginWrapper =>
        val userLogin = userLoginWrapper.user
        for {
          user                   <- userService.loginUser(userLogin)
          userResponseOutWrapper <- user.toUserResponseOutWrapper
          response               <- Ok(userResponseOutWrapper)
        } yield response
      }
  }
}

object AuthenticationHttpRoutes {
  def apply[F[_]: Async: Logger: UserService: JwtAuthenticator: TracingContextBuilder](): AuthenticationHttpRoutes[F] =
    new AuthenticationHttpRoutes[F]()
}
