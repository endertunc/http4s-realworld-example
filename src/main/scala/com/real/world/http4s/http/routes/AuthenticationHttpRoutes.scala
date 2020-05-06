package com.real.world.http4s.http.routes

import org.http4s._
import cats.effect.Async
import cats.implicits._
import com.colisweb.tracing.core.TracingContext
import com.colisweb.tracing.core.TracingContextBuilder
import com.colisweb.tracing.http.server.TracedHttpRoutes
import com.colisweb.tracing.http.server.TracedHttpRoutes.using
import com.real.world.http4s.authentication.JwtAuthenticator
import com.real.world.http4s.http.Http4sAndCirceSupport
import com.real.world.http4s.http.middleware.TracedContextRoutes.TracedContext
import com.real.world.http4s.model.user.{ RegisterUserInput, RegisterUserInputWrapper, UserInputValidator, UserLoginInputWrapper }
import com.real.world.http4s.service.UserService
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger

class AuthenticationHttpRoutes[F[_]: Async: TracingContextBuilder]()(
    implicit L: SelfAwareStructuredLogger[F],
    userService: UserService[F],
    jwtAuthenticator: JwtAuthenticator[F]
) extends Http4sAndCirceSupport[F]
    with UserInputValidator {

  val routes: HttpRoutes[F] = TracedHttpRoutes[F] {
    case (req @ POST -> Root) using implicit0(tracingContext: TracingContext[F]) =>
      req.decode[RegisterUserInputWrapper] { userRegisterWrapper =>
        val userRegisterRequestInput: RegisterUserInput = userRegisterWrapper.user
        for {
          userRegisterRequest    <- validateRegisterUserInput(userRegisterRequestInput)
          user                   <- userService.registerUser(userRegisterRequest)
          userResponseOutWrapper <- user.toUserResponseOutWrapper
          response               <- Ok(userResponseOutWrapper)
        } yield response
      }

    case (req @ POST -> Root / "login") using implicit0(tracingContext: TracingContext[F]) =>
      req.decode[UserLoginInputWrapper] { userLoginWrapper =>
        val userLoginInput = userLoginWrapper.user
        for {
          userLoginRequest       <- validateUserLoginInput(userLoginInput)
          user                   <- userService.loginUser(userLoginRequest)
          userResponseOutWrapper <- user.toUserResponseOutWrapper
          response               <- Ok(userResponseOutWrapper)
        } yield response
      }
  }
}

object AuthenticationHttpRoutes {
  def apply[F[_]: Async: SelfAwareStructuredLogger: UserService: JwtAuthenticator: TracingContextBuilder](): AuthenticationHttpRoutes[F] =
    new AuthenticationHttpRoutes[F]()
}
