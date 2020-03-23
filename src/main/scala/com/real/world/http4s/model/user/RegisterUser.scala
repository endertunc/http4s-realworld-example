package com.real.world.http4s.model.user

import cats.effect.Sync

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

import com.real.world.http4s.model.Instances._
import com.real.world.http4s.model._
import com.real.world.http4s.model._
import com.real.world.http4s.authentication.PasswordHasher
import com.real.world.http4s.authentication.PasswordHasher

import eu.timepit.refined.types.numeric.NonNegInt

final case class RegisterUser(email: Email, password: PlainTextPassword, username: Username)
final case class RegisterUserWrapper(user: RegisterUser)
import io.circe.refined._

import com.real.world.http4s.model.Instances._
object RegisterUser {
  implicit val RegisterUserDecoder: Decoder[RegisterUser] = deriveDecoder[RegisterUser]

  implicit class toUser(registerUser: RegisterUser) {
    def toUser[F[_]: Sync: PasswordHasher]: F[User] =
      User(
        UserId(NonNegInt.MinValue),
        registerUser.email,
        registerUser.password,
        registerUser.username,
        None,
        None
      )
  }

}

object RegisterUserWrapper {
  implicit val RegisterUserWrapperDecoder: Decoder[RegisterUserWrapper] = deriveDecoder[RegisterUserWrapper]

}
