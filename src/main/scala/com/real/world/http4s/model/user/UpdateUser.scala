package com.real.world.http4s.model.user

import cats.effect.Sync

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.refined._
import io.circe.refined._

import com.real.world.http4s.model.Instances._
import com.real.world.http4s.model.Instances._
import com.real.world.http4s.model._
import com.real.world.http4s.authentication.PasswordHasher

import io.chrisdavenport.log4cats.Logger
import mouse.all._

final case class UpdateUser(
    email: Option[Email],
    password: Option[PlainTextPassword],
    username: Option[Username],
    bio: Option[Bio],
    image: Option[Image]
)

final case class UpdateUserWrapper(user: UpdateUser)

object UpdateUser {

  implicit val UpdateUserDecoder: Decoder[UpdateUser] = deriveDecoder[UpdateUser]

  implicit class UserUpdateToUser(userUpdateRequestIn: UpdateUser) {
    import cats.implicits._

    def toUser[F[_]: Sync: Logger](user: User)(implicit tsecPasswordHasher: PasswordHasher[F]): F[User] =
      for {
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

}
