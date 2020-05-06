package com.real.world.http4s.http.routes

import cats.effect.Async
import cats.implicits._

import com.colisweb.tracing.core.{ TracingContext, TracingContextBuilder }

import com.real.world.http4s.http.Http4sAndCirceSupport
import com.real.world.http4s.http.middleware.TracedContextRoutes
import com.real.world.http4s.http.middleware.TracedContextRoutes.{ using, TracedContext, TracedContextRoutes }
import com.real.world.http4s.model._
import com.real.world.http4s.model.profile.ProfileResponseOutWrapper
import com.real.world.http4s.service.ProfileService

import io.chrisdavenport.log4cats.SelfAwareStructuredLogger

class ProfileRoutes[F[_]: Async: TracingContextBuilder]()(implicit L: SelfAwareStructuredLogger[F], profileService: ProfileService[F])
    extends Http4sAndCirceSupport[F] {

  object UsernameVar {
    def unapply(slugStr: String): Option[Username] = Username.from(slugStr).toOption
  }

  val routes: TracedContextRoutes[F, UserId] =
    TracedContextRoutes.of[F, UserId] {
      case (GET -> Root / UsernameVar(followeeUsername)) using TracedContext(userId, implicit0(context: TracingContext[F])) =>
        for {
          profile  <- profileService.findProfileByUsername(followeeUsername, userId)
          response <- Ok(ProfileResponseOutWrapper(profile))
        } yield response

      case (POST -> Root / UsernameVar(followeeUsername) / "follow") using TracedContext(userId, implicit0(context: TracingContext[F])) =>
        for {
          _        <- profileService.follow(followeeUsername, userId)
          profile  <- profileService.findProfileByUsername(followeeUsername, userId)
          response <- Ok(ProfileResponseOutWrapper(profile))
        } yield response

      case (DELETE -> Root / UsernameVar(followeeUsername) / "follow") using TracedContext(userId, implicit0(context: TracingContext[F])) =>
        for {
          _        <- profileService.unfollow(followeeUsername, userId)
          profile  <- profileService.findProfileByUsername(followeeUsername, userId)
          response <- Ok(ProfileResponseOutWrapper(profile))
        } yield response
    }

}

object ProfileRoutes {
  def apply[F[_]: Async: SelfAwareStructuredLogger: ProfileService: TracingContextBuilder](): ProfileRoutes[F] = new ProfileRoutes[F]()
}
