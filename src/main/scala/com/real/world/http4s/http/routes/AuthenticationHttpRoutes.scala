package com.real.world.http4s.http.routes

import com.real.world.http4s.http.BaseHttp4s
import com.real.world.http4s.model.user.{ RegisterUser, _ }
import com.real.world.http4s.security.{ JwtAuthenticator, PasswordHasher }
import com.real.world.http4s.http.BaseHttp4s
import com.real.world.http4s.model.user.{ RegisterUser, RegisterUserWrapper, UserLoginWrapper }
import com.real.world.http4s.security.{ JwtAuthenticator, PasswordHasher }
import com.real.world.http4s.service.UserService

import cats.effect.Async
import cats.implicits._

import org.http4s._
import io.chrisdavenport.log4cats.Logger

class AuthenticationHttpRoutes[F[_]: Async: Logger]()(
    implicit userService: UserService[F],
    passwordHasher: PasswordHasher[F],
    jwtAuthenticator: JwtAuthenticator[F]
) extends BaseHttp4s[F] {

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {

    case req @ POST -> Root =>
      req.decode[RegisterUserWrapper] { userRegisterWrapper =>
        val userRegisterRequestIn: RegisterUser = userRegisterWrapper.user
        (for {
          user                   <- userService.registerUser(userRegisterRequestIn)
          userResponseOutWrapper <- user.toUserResponseOutWrapper()
        } yield userResponseOutWrapper).toResponse
      }

    case req @ POST -> Root / "login" =>
      req.decode[UserLoginWrapper] { userLoginWrapper =>
        val userLogin = userLoginWrapper.user
        (for {
          user                   <- userService.loginUser(userLogin)
          userResponseOutWrapper <- user.toUserResponseOutWrapper()
        } yield userResponseOutWrapper).toResponse
      }
  }
}

object AuthenticationHttpRoutes {
  def apply[F[_]: Async: Logger: UserService: PasswordHasher: JwtAuthenticator](): AuthenticationHttpRoutes[F] = new AuthenticationHttpRoutes[F]()
}
