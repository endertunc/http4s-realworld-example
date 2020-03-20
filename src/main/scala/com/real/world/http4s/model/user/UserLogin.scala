package com.real.world.http4s.model.user

import com.real.world.http4s.json.CirceSchemaValidatorWrapper
import com.real.world.http4s.model.user.User.{ Email, PlainTextPassword }
import com.real.world.http4s.json.CirceSchemaValidatorWrapper
import com.real.world.http4s.model.user.User.{ Email, PlainTextPassword }
import json.Schema

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

final case class UserLogin(email: Email, password: PlainTextPassword)
final case class UserLoginWrapper(user: UserLogin)

object UserLogin {
  implicit val UserLoginDecoder: Decoder[UserLogin] = deriveDecoder[UserLogin]
}

object UserLoginWrapper {
  implicit val UserLoginWrapperDecoder: Decoder[UserLoginWrapper]       = deriveDecoder[UserLoginWrapper]
  implicit val UserRegisterWrapperCirceSchema: Schema[UserLoginWrapper] = json.Json.schema[UserLoginWrapper]
  implicit val UserRegisterWrapperValidatorImpl: CirceSchemaValidatorWrapper[UserLoginWrapper] =
    new CirceSchemaValidatorWrapper[UserLoginWrapper]("UserLoginWrapper")
}
