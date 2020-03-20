package com.real.world.http4s.model.user

import com.real.world.http4s.json.CirceSchemaValidatorWrapper
import com.real.world.http4s.model.user.User.{ Email, PlainTextPassword, UserId, Username }
import com.real.world.http4s.security.PasswordHasher
import com.real.world.http4s.json.CirceSchemaValidatorWrapper
import com.real.world.http4s.model.user.User.{ Email, PlainTextPassword, Username }
import com.real.world.http4s.security.PasswordHasher
import json.Schema

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

import cats.effect.Sync

final case class RegisterUser(email: Email, password: PlainTextPassword, username: Username)
final case class RegisterUserWrapper(user: RegisterUser)

object RegisterUser {
  implicit val RegisterUserDecoder: Decoder[RegisterUser] = deriveDecoder[RegisterUser]

  implicit class toUser(registerUser: RegisterUser) {
    def toUser[F[_]: Sync: PasswordHasher]: F[User] =
      User(
        UserId(-1),
        registerUser.email,
        registerUser.password,
        registerUser.username,
        None,
        None
      )
  }

}

object RegisterUserWrapper {
  implicit val RegisterUserWrapperDecoder: Decoder[RegisterUserWrapper]    = deriveDecoder[RegisterUserWrapper]
  implicit val RegisterUserWrapperCirceSchema: Schema[RegisterUserWrapper] = json.Json.schema[RegisterUserWrapper]
  implicit val RegisterUserWrapperValidatorImpl: CirceSchemaValidatorWrapper[RegisterUserWrapper] =
    new CirceSchemaValidatorWrapper[RegisterUserWrapper]("RegisterUserWrapper")

}
