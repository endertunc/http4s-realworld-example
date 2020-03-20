package com.real.world.http4s.security

import com.real.world.http4s.model.user.User.{ HashedPassword, PlainTextPassword }

trait PasswordHasher[F[_]] {
  def hash(plainTextPassword: PlainTextPassword): F[HashedPassword]
  def checkHash(plainTextPassword: PlainTextPassword, hashedPassword: HashedPassword): F[Boolean]
}
