package com.real.world.http4s.http.routes

import com.real.world.http4s.http.BaseHttp4s
import com.real.world.http4s.model.user.User.UserId
import com.real.world.http4s.model.user.{ UpdateUserWrapper, UserResponseWrapper }
import com.real.world.http4s.security.{ JwtAuthenticator, PasswordHasher }
import com.real.world.http4s.http.BaseHttp4s
import com.real.world.http4s.model.user.{ UpdateUserWrapper, UserResponseWrapper }
import com.real.world.http4s.security.{ JwtAuthenticator, PasswordHasher }
import com.real.world.http4s.service.UserService

import cats.effect.Async
import cats.implicits._

import org.http4s.AuthedRoutes
import io.chrisdavenport.log4cats.Logger

class UserRoutes[F[_]: Async: Logger]()(
    implicit userService: UserService[F],
    passwordHasher: PasswordHasher[F],
    jwtAuthenticator: JwtAuthenticator[F]
) extends BaseHttp4s[F] {

  val routes: AuthedRoutes[UserId, F] =
    AuthedRoutes.of {
      case GET -> Root as userId =>
        val response: F[UserResponseWrapper] = for {
          user                   <- userService.findUserById(userId)
          userResponseOutWrapper <- user.toUserResponseOutWrapper
        } yield userResponseOutWrapper
        response // implicit conversation from F[A] to F[Response[F]]

      case req @ PUT -> Root as userId =>
        req.req.decode[UpdateUserWrapper] { updateUserWrapper =>
          val updateUserRequestIn = updateUserWrapper.user
          val response: F[UserResponseWrapper] = for {
            updateResponse         <- userService.updateUser(updateUserRequestIn, userId)
            userResponseOutWrapper <- updateResponse.toUserResponseOutWrapper()
          } yield userResponseOutWrapper: UserResponseWrapper
          response // implicit conversation from F[A] to F[Response[F]]
        }
    }

}

object UserRoutes {
  def apply[F[_]: Async: Logger: UserService: PasswordHasher: JwtAuthenticator](): UserRoutes[F] = new UserRoutes[F]()
}
