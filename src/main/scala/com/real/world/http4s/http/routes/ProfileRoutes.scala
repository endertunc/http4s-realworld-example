package com.real.world.http4s.http.routes

import com.real.world.http4s.http.BaseHttp4s
import com.real.world.http4s.model.user.User.{ UserId, Username }
import com.real.world.http4s.service.ProfileService
import com.real.world.http4s.http.BaseHttp4s
import com.real.world.http4s.model.profile.ProfileResponseOutWrapper
import com.real.world.http4s.service.ProfileService

import cats.effect.Async
import cats.implicits._

import org.http4s.AuthedRoutes
import io.chrisdavenport.log4cats.Logger

class ProfileRoutes[F[_]: Async: Logger]()(implicit profileService: ProfileService[F]) extends BaseHttp4s[F] {

  val routes: AuthedRoutes[UserId, F] =
    AuthedRoutes.of {
      case GET -> Root / followeeUsername as userId =>
        (for {
          profile <- profileService.findProfileByUsername(Username(followeeUsername), userId)
        } yield ProfileResponseOutWrapper(profile)).toResponse

      case POST -> Root / followeeUsername / "follow" as userId =>
        (for {
          _       <- profileService.follow(Username(followeeUsername), userId)
          profile <- profileService.findProfileByUsername(Username(followeeUsername), userId)
        } yield ProfileResponseOutWrapper(profile)).toResponse

      case DELETE -> Root / followeeUsername / "follow" as userId =>
        (for {
          _       <- profileService.unfollow(Username(followeeUsername), userId)
          profile <- profileService.findProfileByUsername(Username(followeeUsername), userId)
        } yield ProfileResponseOutWrapper(profile)).toResponse
    }

}

object ProfileRoutes {
  def apply[F[_]: Async: Logger: ProfileService](): ProfileRoutes[F] = new ProfileRoutes[F]()
}
