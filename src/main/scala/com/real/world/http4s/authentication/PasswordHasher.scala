package com.real.world.http4s.authentication

import com.real.world.http4s.model._

trait PasswordHasher[F[_]] {
  def hash(plainTextPassword: PlainTextPassword): F[HashedPassword]
  def checkHash(plainTextPassword: PlainTextPassword, hashedPassword: HashedPassword): F[Boolean]
}
