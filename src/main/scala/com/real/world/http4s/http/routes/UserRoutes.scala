package com.real.world.http4s.http.routes

import cats.effect.Async
import cats.implicits._

import com.colisweb.tracing.core.{ TracingContext, TracingContextBuilder }

import com.real.world.http4s.authentication.JwtAuthenticator
import com.real.world.http4s.http.Http4sAndCirceSupport
import com.real.world.http4s.http.middleware.TracedContextRoutes
import com.real.world.http4s.http.middleware.TracedContextRoutes.{ using, TracedContext, TracedContextRoutes }
import com.real.world.http4s.model.UserId
import com.real.world.http4s.model.user.{ UpdateUserWrapper, UserResponseWrapper }
import com.real.world.http4s.service.UserService

import io.chrisdavenport.log4cats.SelfAwareStructuredLogger

class UserRoutes[F[_]: Async: TracingContextBuilder]()(
    implicit L: SelfAwareStructuredLogger[F],
    userService: UserService[F],
    jwtAuthenticator: JwtAuthenticator[F]
) extends Http4sAndCirceSupport[F] {

  val routes: TracedContextRoutes[F, UserId] =
    TracedContextRoutes.of[F, UserId] {
      case (GET -> Root) using TracedContext(userId, implicit0(context: TracingContext[F])) =>
        for {
          user                   <- userService.findUserById(userId)
          userResponseOutWrapper <- user.toUserResponseOutWrapper
          response               <- Ok(userResponseOutWrapper)
        } yield response

      case (req @ PUT -> Root) using TracedContext(userId, implicit0(context: TracingContext[F])) =>
        req.decode[UpdateUserWrapper] { updateUserWrapper =>
          val updateUserRequestIn = updateUserWrapper.user
          for {
            updateResponse         <- userService.updateUser(updateUserRequestIn, userId)
            userResponseOutWrapper <- updateResponse.toUserResponseOutWrapper
            response               <- Ok(userResponseOutWrapper: UserResponseWrapper)
          } yield response
        }
    }

}

object UserRoutes {
  def apply[F[_]: Async: SelfAwareStructuredLogger: UserService: JwtAuthenticator: TracingContextBuilder](): UserRoutes[F] = new UserRoutes[F]()
}
