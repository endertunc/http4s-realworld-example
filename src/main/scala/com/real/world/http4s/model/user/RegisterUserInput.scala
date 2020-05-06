package com.real.world.http4s.model.user

import cats.effect.Sync

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

import com.real.world.http4s.authentication.PasswordHasher
import com.real.world.http4s.authentication.PasswordHasher
import com.real.world.http4s.model.NewTypeImplicits._
import com.real.world.http4s.model.{ Email, PlainTextPassword, UserId, Username }

import eu.timepit.refined.types.numeric.NonNegInt

final case class RegisterUserInput(email: String, password: String, username: String)
final case class RegisterUserInputWrapper(user: RegisterUserInput)
final case class RegisterUserRequest(email: Email, username: Username, password: PlainTextPassword)

import com.real.world.http4s.model.NewTypeImplicits._
object RegisterUserInput {
  implicit val RegisterUserDecoder: Decoder[RegisterUserInput] = deriveDecoder[RegisterUserInput]

  // ToDo move it somewhere else maybe?
  implicit class toUser(registerUser: RegisterUserRequest) {
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

object RegisterUserInputWrapper {
  implicit val RegisterUserWrapperDecoder: Decoder[RegisterUserInputWrapper] = deriveDecoder[RegisterUserInputWrapper]

}
