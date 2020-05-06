package com.real.world.http4s.model.user

import cats.effect.Sync
import cats.implicits._

import com.real.world.http4s.authentication.{ JwtAuthenticator, PasswordHasher }
import com.real.world.http4s.model._

import eu.timepit.refined.auto._
import eu.timepit.refined.types.numeric.NonNegInt

final case class User(
    id: UserId,
    email: Email,
    hashedPassword: HashedPassword,
    username: Username,
    bio: Option[Bio],
    image: Option[Image] = Some(Image("https://www.deskpro.com/assets/images/apps/app-logos/gravatar.svg"))
)

object User {

  def apply[F[_]: Sync](id: UserId, email: Email, plainTextPassword: PlainTextPassword, username: Username, bio: Option[Bio], image: Option[Image])(
      implicit passwordHasher: PasswordHasher[F]
  ): F[User] =
    for {
      hashedPassword <- passwordHasher.hash(plainTextPassword)
      user = new User(id, email, hashedPassword, username, bio, image)
    } yield user

  def apply[F[_]: Sync: PasswordHasher](email: Email, username: Username, plainTextPassword: PlainTextPassword): F[User] = User(
    UserId(NonNegInt.MinValue),
    email,
    plainTextPassword,
    username,
    None,
    None
  )

  implicit class userToUserResponseOutWrapper(user: User) {
    def toUserResponseOutWrapper[F[_]: Sync]()(implicit jwtAuthenticator: JwtAuthenticator[F]): F[UserResponseWrapper] =
      for {
        token  <- jwtAuthenticator.generateJwt(user.id)
        result <- UserResponseWrapper(UserResponse(user.email, token, user.username, user.bio, user.image)).pure[F]
      } yield result
  }

}
