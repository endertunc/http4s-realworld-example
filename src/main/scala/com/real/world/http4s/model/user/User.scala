package com.real.world.http4s.model.user

import cats.effect.Sync
import cats.implicits._

import com.real.world.http4s.model._
import com.real.world.http4s.authentication.{ JwtAuthenticator, PasswordHasher }

import eu.timepit.refined.auto._

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

  implicit class userToUserResponseOutWrapper(user: User) {
    def toUserResponseOutWrapper[F[_]: Sync]()(implicit jwtAuthenticator: JwtAuthenticator[F]): F[UserResponseWrapper] =
      for {
        token  <- jwtAuthenticator.generateJwt(user.id)
        result <- UserResponseWrapper(UserResponse(user.email, token, user.username, user.bio, user.image)).pure[F]
      } yield result
  }

}
