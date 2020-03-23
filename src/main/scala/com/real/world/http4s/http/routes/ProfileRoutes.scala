package com.real.world.http4s.http.routes

import cats.effect.Async
import cats.implicits._
import com.colisweb.tracing.core.{ TracingContext, TracingContextBuilder }
import com.real.world.http4s.http.middleware.AuthedTracedRoutes.{ using, AuthedTraceContext, AuthedTracedRoutes }
import com.real.world.http4s.http.BaseHttp4s
import com.real.world.http4s.http.middleware.AuthedTracedRoutes
import com.real.world.http4s.model._
import com.real.world.http4s.model.profile.ProfileResponseOutWrapper
import com.real.world.http4s.service.ProfileService
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineV
import io.chrisdavenport.log4cats.Logger

class ProfileRoutes[F[_]: Async: Logger: TracingContextBuilder]()(implicit profileService: ProfileService[F]) extends BaseHttp4s[F] {

  object UsernameVar {
    def unapply(slugStr: String): Option[Username] = refineV[NonEmpty](slugStr).toOption.map(Username.apply)
  }

  val routes: AuthedTracedRoutes[F] =
    AuthedTracedRoutes.of[F] {
      case (GET -> Root / UsernameVar(followeeUsername)) using AuthedTraceContext(userId, implicit0(context: TracingContext[F])) =>
        for {
          profile  <- profileService.findProfileByUsername(followeeUsername, userId)
          response <- Ok(ProfileResponseOutWrapper(profile))
        } yield response

      case (POST -> Root / UsernameVar(followeeUsername) / "follow") using AuthedTraceContext(userId, implicit0(context: TracingContext[F])) =>
        for {
          _        <- profileService.follow(followeeUsername, userId)
          profile  <- profileService.findProfileByUsername(followeeUsername, userId)
          response <- Ok(ProfileResponseOutWrapper(profile))
        } yield response

      case (DELETE -> Root / UsernameVar(followeeUsername) / "follow") using AuthedTraceContext(userId, implicit0(context: TracingContext[F])) =>
        for {
          _        <- profileService.unfollow(followeeUsername, userId)
          profile  <- profileService.findProfileByUsername(followeeUsername, userId)
          response <- Ok(ProfileResponseOutWrapper(profile))
        } yield response
    }

}

object ProfileRoutes {
  def apply[F[_]: Async: Logger: ProfileService: TracingContextBuilder](): ProfileRoutes[F] = new ProfileRoutes[F]()
}
