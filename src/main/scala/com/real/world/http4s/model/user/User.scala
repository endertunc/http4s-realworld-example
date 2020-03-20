package com.real.world.http4s.model.user

import com.real.world.http4s.model.user.User.{ Bio, Email, HashedPassword, Image, UserId, Username }
import com.real.world.http4s.security.{ JwtAuthenticator, PasswordHasher }
import com.real.world.http4s.security.{ JwtAuthenticator, PasswordHasher }

import io.circe.Codec
import io.circe.generic.extras.semiauto.deriveUnwrappedCodec

import cats.effect.Sync
import cats.implicits._

final case class User(
    id: UserId,
    email: Email,
    hashedPassword: HashedPassword,
    username: Username,
    bio: Option[Bio],
    image: Option[Image] = Some(Image("https://www.deskpro.com/assets/images/apps/app-logos/gravatar.svg"))
)

object User {

  // Value classes
  case class UserId(value: Int) extends AnyVal
  case class Username(value: String) extends AnyVal
  case class PlainTextPassword(value: String) extends AnyVal
  case class HashedPassword(value: String) extends AnyVal
  case class Email(value: String) extends AnyVal
  case class Bio(value: String) extends AnyVal
  case class Image(value: String) extends AnyVal
  case class Token(value: String) extends AnyVal

  implicit val UserIdCodec: Codec[UserId]                       = deriveUnwrappedCodec[UserId]
  implicit val UsernameCodec: Codec[Username]                   = deriveUnwrappedCodec[Username]
  implicit val PlainTextPasswordCodec: Codec[PlainTextPassword] = deriveUnwrappedCodec[PlainTextPassword]
  implicit val EmailCodec: Codec[Email]                         = deriveUnwrappedCodec[Email]
  implicit val BioCodec: Codec[Bio]                             = deriveUnwrappedCodec[Bio]
  implicit val ImageCodec: Codec[Image]                         = deriveUnwrappedCodec[Image]
  implicit val TokenCodec: Codec[Token]                         = deriveUnwrappedCodec[Token]

  def apply[F[_]: Sync](id: UserId, email: Email, plainTextPassword: PlainTextPassword, username: Username, bio: Option[Bio], image: Option[Image])(
      implicit passwordHasher: PasswordHasher[F]
  ): F[User] =
    for {
      hashedPassword <- passwordHasher.hash(plainTextPassword)
      user = new User(id, email, hashedPassword, username, bio, image)
    } yield user

  implicit class userToUserResponseOutWrapper(user: User) {
    def toUserResponseOutWrapper[F[_]: Sync]()(implicit jwtAuthenticator: JwtAuthenticator[F]): F[UserResponseWrapper] =
      for {
        token  <- jwtAuthenticator.generateJwt(user.id)
        result <- UserResponseWrapper(UserResponse(user.email, Token(token), user.username, user.bio, user.image)).pure[F]
      } yield result
  }

}
