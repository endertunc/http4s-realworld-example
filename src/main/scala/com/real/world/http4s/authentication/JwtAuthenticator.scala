package com.real.world.http4s.authentication

import com.real.world.http4s.model._

trait JwtAuthenticator[F[_]] {
  def generateJwt(userId: UserId): F[Token]
  def verify(jwt: String): F[UserId]
}
