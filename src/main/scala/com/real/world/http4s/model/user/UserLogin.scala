package com.real.world.http4s.model.user

import io.circe.Decoder
import io.circe.generic.semiauto._
import io.circe.refined._

import com.real.world.http4s.model.Instances._
import com.real.world.http4s.model._

final case class UserLogin(email: Email, password: PlainTextPassword)
final case class UserLoginWrapper(user: UserLogin)

object UserLogin {
  implicit val UserLoginDecoder: Decoder[UserLogin] = deriveDecoder[UserLogin]
}

object UserLoginWrapper {
  implicit val UserLoginWrapperDecoder: Decoder[UserLoginWrapper] = deriveDecoder[UserLoginWrapper]

}
