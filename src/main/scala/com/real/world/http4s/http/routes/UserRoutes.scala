package com.real.world.http4s.http.routes

import cats.effect.Async
import cats.implicits._
import com.colisweb.tracing.core.{ TracingContext, TracingContextBuilder }
import com.real.world.http4s.http.middleware.AuthedTracedRoutes.{ using, AuthedTraceContext, AuthedTracedRoutes }
import com.real.world.http4s.http.BaseHttp4s
import com.real.world.http4s.http.middleware.AuthedTracedRoutes
import com.real.world.http4s.model.user.{ UpdateUserWrapper, UserResponseWrapper }
import com.real.world.http4s.authentication.JwtAuthenticator
import com.real.world.http4s.service.UserService
import io.chrisdavenport.log4cats.Logger

class UserRoutes[F[_]: Async: Logger: TracingContextBuilder]()(implicit userService: UserService[F], jwtAuthenticator: JwtAuthenticator[F])
    extends BaseHttp4s[F] {

  val routes: AuthedTracedRoutes[F] =
    AuthedTracedRoutes.of[F] {
      case (GET -> Root) using AuthedTraceContext(userId, implicit0(context: TracingContext[F])) =>
        for {
          user                   <- userService.findUserById(userId)
          userResponseOutWrapper <- user.toUserResponseOutWrapper
          response               <- Ok(userResponseOutWrapper)
        } yield response

      case (req @ PUT -> Root) using AuthedTraceContext(userId, implicit0(context: TracingContext[F])) =>
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
  def apply[F[_]: Async: Logger: UserService: JwtAuthenticator: TracingContextBuilder](): UserRoutes[F] = new UserRoutes[F]()
}
