package com.real.world.http4s.authentication

import cats.effect.Sync
import cats.implicits._

import com.real.world.http4s.model._

import tsec.common.Verified
import tsec.passwordhashers.jca.SCrypt
import tsec.passwordhashers.{ PasswordHash, PasswordHashAPI }

class SCryptPasswordHasher[F[_]: Sync](hasher: PasswordHashAPI[SCrypt])(implicit P: tsec.passwordhashers.PasswordHasher[F, SCrypt])
    extends PasswordHasher[F] {

  override def hash(plainTextPassword: PlainTextPassword): F[HashedPassword] =
    hasher.hashpw[F](plainTextPassword.value.value).map(HashedPassword.apply)

  override def checkHash(plainTextPassword: PlainTextPassword, hashedPassword: HashedPassword): F[Boolean] =
    hasher.checkpw[F](plainTextPassword.value.value, PasswordHash.apply[SCrypt](hashedPassword.value)).map(_ == Verified)

}
