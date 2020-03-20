package com.real.world.http4s.generators

import com.real.world.http4s.security.PasswordHasher
import com.real.world.http4s.model.user
import com.real.world.http4s.model.user.User
import com.real.world.http4s.security.PasswordHasher
import org.scalacheck.Gen

import cats.effect.IO

object UserGenerator extends ValueClassGens {

  private def userGen()(implicit tsecPasswordHasher: PasswordHasher[IO]): Gen[IO[User]] =
    for {
      userId   <- userIDGen
      email    <- emailGen
      password <- passwordGen
      username <- usernameGen
      bio      <- bioGen
      image    <- imageGen
    } yield user.User[IO](
      userId,
      email,
      password,
      username,
      bio,
      image
    )

  def generateUser()(implicit tsecPasswordHasher: PasswordHasher[IO]): User = userGen.sample.get.unsafeRunSync()

}
