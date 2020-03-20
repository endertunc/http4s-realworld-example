package com.real.world.http4s.security

import com.real.world.http4s.model.user.User.{ HashedPassword, PlainTextPassword }
import tsec.common.Verified
import tsec.passwordhashers.jca.SCrypt
import tsec.passwordhashers.{ PasswordHash, PasswordHashAPI }

import cats.effect.Sync
import cats.implicits._

class SCryptPasswordHasher[F[_]: Sync](hasher: PasswordHashAPI[SCrypt])(implicit P: tsec.passwordhashers.PasswordHasher[F, SCrypt])
    extends PasswordHasher[F] {

  override def hash(plainTextPassword: PlainTextPassword): F[HashedPassword] = hasher.hashpw[F](plainTextPassword.value).map(HashedPassword)

  override def checkHash(plainTextPassword: PlainTextPassword, hashedPassword: HashedPassword): F[Boolean] =
    hasher.checkpw[F](plainTextPassword.value, PasswordHash.apply[SCrypt](hashedPassword.value)).map(_ == Verified)

}
