package com.real.world.http4s.model.user

import com.real.world.http4s.json.CirceSchemaValidatorWrapper
import com.real.world.http4s.model.UserValidators
import com.real.world.http4s.model.user.User.{ Bio, Email, Image, PlainTextPassword, Username }
import com.real.world.http4s.security.PasswordHasher
import com.real.world.http4s.json.CirceSchemaValidatorWrapper
import com.real.world.http4s.model.UserValidators
import com.real.world.http4s.model.user.User.{ Bio, Email, Image, PlainTextPassword, Username }
import com.real.world.http4s.security.PasswordHasher
import io.chrisdavenport.log4cats.Logger
import json.Schema
import mouse.all._

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

import cats.effect.Sync

final case class UpdateUser(
    email: Option[Email],
    password: Option[PlainTextPassword],
    username: Option[Username],
    bio: Option[Bio],
    image: Option[Image]
)

final case class UpdateUserWrapper(user: UpdateUser)

object UpdateUser extends UserValidators {

  implicit val UpdateUserDecoder: Decoder[UpdateUser] = deriveDecoder[UpdateUser]

  implicit class UserUpdateToUser(userUpdateRequestIn: UpdateUser) {
    import cats.implicits._

    def toUser[F[_]: Sync: Logger](user: User)(implicit tsecPasswordHasher: PasswordHasher[F]): F[User] =
      for {
        _ <- validateUpdateUser(userUpdateRequestIn)
        hashedPassword <- userUpdateRequestIn.password match {
          case Some(password) => tsecPasswordHasher.hash(password)
          case None           => user.hashedPassword |> (Sync[F].delay(_))
        }
      } yield {
        new User(
          id             = user.id,
          email          = userUpdateRequestIn.email.getOrElse(user.email),
          hashedPassword = hashedPassword,
          username       = userUpdateRequestIn.username.getOrElse(user.username),
          bio            = userUpdateRequestIn.bio.map(Option.apply).getOrElse(user.bio),
          image          = userUpdateRequestIn.image.map(Option.apply).getOrElse(user.image)
        )
      }
  }

}

object UpdateUserWrapper {
  implicit val UpdateUserWrapperDecoder: Decoder[UpdateUserWrapper] = deriveDecoder[UpdateUserWrapper]
  implicit val UpdateUserWrapperSchema: Schema[UpdateUserWrapper]   = json.Json.schema[UpdateUserWrapper]
  implicit val UpdateUserWrapperValidatorImpl: CirceSchemaValidatorWrapper[UpdateUserWrapper] =
    new CirceSchemaValidatorWrapper[UpdateUserWrapper]("UpdateUserWrapper")

}
