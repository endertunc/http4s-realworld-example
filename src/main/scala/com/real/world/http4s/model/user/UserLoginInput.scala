package com.real.world.http4s.model.user

import io.circe.Decoder
import io.circe.generic.semiauto._

import com.real.world.http4s.model.NewTypeImplicits._
import com.real.world.http4s.model._

final case class UserLoginInput(email: String, password: String)
final case class UserLoginInputWrapper(user: UserLoginInput)
final case class UserLoginRequest(email: Email, password: PlainTextPassword)

object UserLoginInput {
  implicit val UserLoginDecoder: Decoder[UserLoginInput] = deriveDecoder[UserLoginInput]
}

object UserLoginInputWrapper {
  implicit val UserLoginWrapperDecoder: Decoder[UserLoginInputWrapper] = deriveDecoder[UserLoginInputWrapper]

}
