package com.real.world.http4s.model.user

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import io.circe.refined._

import com.real.world.http4s.model.NewTypeImplicits._
import com.real.world.http4s.model._

final case class UserResponse(email: Email, token: Token, username: Username, bio: Option[Bio], image: Option[Image])
final case class UserResponseWrapper(user: UserResponse)

object UserResponse {
  implicit val UserResponseEncoder: Encoder[UserResponse] = deriveEncoder[UserResponse]
}

object UserResponseWrapper {
  implicit val UserResponseWrapperEncoder: Encoder[UserResponseWrapper] = deriveEncoder[UserResponseWrapper]
}
