package com.real.world.http4s.security

import com.real.world.http4s.model.user.User.UserId

trait JwtAuthenticator[F[_]] {
  def generateJwt(userId: UserId): F[String]
  def verify(jwt: String): F[UserId]
}
