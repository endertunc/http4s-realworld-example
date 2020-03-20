package com.real.world.http4s.model.user

import User.{ Bio, Email, Image, Token, Username }

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

final case class UserResponse(email: Email, token: Token, username: Username, bio: Option[Bio], image: Option[Image])
final case class UserResponseWrapper(user: UserResponse)

object UserResponse {
  implicit val UserResponseEncoder: Encoder[UserResponse] = deriveEncoder[UserResponse]
}

object UserResponseWrapper {
  implicit val UserResponseWrapperEncoder: Encoder[UserResponseWrapper] = deriveEncoder[UserResponseWrapper]
}
