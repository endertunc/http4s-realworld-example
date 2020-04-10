package com.real.world.http4s.http.routes

import cats.effect.Async
import cats.implicits._
import com.colisweb.tracing.core.{ TracingContext, TracingContextBuilder }
import com.real.world.http4s.http.middleware.AuthedTracedRoutes.using
import com.real.world.http4s.http.middleware.AuthedTracedRoutes.{ AuthedTraceContext, AuthedTracedRoutes }
import com.real.world.http4s.http.BaseHttp4s
import com.real.world.http4s.http.middleware.AuthedTracedRoutes
import com.real.world.http4s.model._
import com.real.world.http4s.model.profile.ProfileResponseOutWrapper
import com.real.world.http4s.service.ProfileService
import io.chrisdavenport.log4cats.Logger

class ExampleAuthedTracedRoutes[F[_]: Async: Logger: TracingContextBuilder]()(implicit profileService: ProfileService[F]) extends BaseHttp4s[F] {

  object UsernameVar {
    def unapply(slugStr: String): Option[Username] = Username.from(slugStr).toOption
  }

  val routes: AuthedTracedRoutes[F] = AuthedTracedRoutes.of {
    case (req @ GET -> Root / UsernameVar(followeeUsername)) using AuthedTraceContext(
          implicit0(userId: UserId),
          implicit0(context: TracingContext[F])
        ) =>
      for {
        profile  <- profileService.findProfileByUsername(followeeUsername, userId)
        response <- Ok(ProfileResponseOutWrapper(profile))
      } yield response
  }
}

object ExampleAuthedTracedRoutes {
  def apply[F[_]: Async: Logger: ProfileService: TracingContextBuilder](): ExampleAuthedTracedRoutes[F] = new ExampleAuthedTracedRoutes[F]()
}
