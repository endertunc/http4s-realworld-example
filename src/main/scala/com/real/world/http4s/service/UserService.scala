package com.real.world.http4s.service

import com.real.world.http4s.AppError.{ PasswordHashFailed, UserNotFound }
import com.real.world.http4s.model.UserValidators
import com.real.world.http4s.model.profile.{ IsFollowing, Profile }
import com.real.world.http4s.model.user.User.{ UserId, Username }
import com.real.world.http4s.model.user.{ RegisterUser, UpdateUser, User, UserLogin }
import com.real.world.http4s.repository.algebra.UserRepositoryAlgebra
import com.real.world.http4s.security.PasswordHasher
import com.real.world.http4s.model.UserValidators
import com.real.world.http4s.model.profile.{ IsFollowing, Profile }
import com.real.world.http4s.model.user.{ RegisterUser, UpdateUser, User, UserLogin }
import com.real.world.http4s.repository.algebra.UserRepositoryAlgebra
import com.real.world.http4s.security.PasswordHasher

import cats.data.NonEmptyList
import cats.effect.{ Async, Sync }
import cats.implicits._
import io.chrisdavenport.log4cats.Logger

class UserService[F[_]: Async: Logger]()(implicit userRepositoryAlgebra: UserRepositoryAlgebra[F], passwordHasher: PasswordHasher[F])
    extends UserValidators {

  def registerUser(registerUser: RegisterUser): F[User] =
    for {
      _         <- Logger[F].trace(s"Trying to register user new user with email [${registerUser.email}] and username [${registerUser.username}]")
      _         <- validateRegisterUser[F](registerUser)
      user      <- registerUser.toUser
      savedUser <- userRepositoryAlgebra.insertUser(user)
      _         <- Logger[F].trace(s"User with email [${registerUser.email}] and username [${registerUser.username}] successfully registered")
    } yield savedUser

  def loginUser(userLogin: UserLogin): F[User] =
    for {
      _         <- Logger[F].trace(s"User with email [${userLogin.email}] trying to login")
      userAsOpt <- userRepositoryAlgebra.findUserByEmail(userLogin.email)
      user      <- Sync[F].fromOption(userAsOpt, UserNotFound(userLogin.email.value))
      loggedInUser <- Sync[F].ifM(passwordHasher.checkHash(userLogin.password, user.hashedPassword))(
        user.pure[F],
        Logger[F].warn(s"User [${user.id}] failed to login") *>
        PasswordHashFailed("In valid email or password").raiseError[F, User]
      )
      _ <- Logger[F].trace(s"User with email [${userLogin.email}] successfully logged in")
    } yield loggedInUser

  def findUserById(userId: UserId): F[User] =
    for {
      _         <- Logger[F].trace(s"Trying to find user by id [$userId]")
      userAsOpt <- userRepositoryAlgebra.findUserById(userId)
      user      <- Sync[F].fromOption(userAsOpt, UserNotFound(userId.value.toString))
      _         <- Logger[F].trace(s"Found user with id [$userId]")
    } yield user

  def findUserByUsername(username: Username): F[User] =
    for {
      _         <- Logger[F].trace(s"Trying to find user by username [$username]")
      userAsOpt <- userRepositoryAlgebra.findUserByUsername(username)
      user      <- Sync[F].fromOption(userAsOpt, UserNotFound(username.value))
      _         <- Logger[F].trace(s"Found user with id [${user.id}]")
    } yield user

  def updateUser(updateUser: UpdateUser, userId: UserId): F[User] =
    for {
      _                <- validateUpdateUser(updateUser)
      existingUser     <- findUserById(userId)
      user             <- updateUser.toUser[F](existingUser)
      updatedUserAsOpt <- userRepositoryAlgebra.updateUser(user)
      updatedUser      <- Sync[F].fromOption(updatedUserAsOpt, UserNotFound(userId.value.toString))
    } yield updatedUser

  def findProfilesByUserId(usersIds: NonEmptyList[UserId], maybeFollower: Option[UserId]): F[Map[UserId, Profile]] =
    maybeFollower.fold(findProfilesByUserId(usersIds))(follower => userRepositoryAlgebra.findProfilesByUserId(usersIds, follower))

  private def findProfilesByUserId(usersIds: NonEmptyList[UserId]): F[Map[UserId, Profile]] =
    userRepositoryAlgebra
      .findUsersByUserId(usersIds)
      .map(users => users.map(user => user.id -> Profile(user, IsFollowing.NotFollowing)).toMap)

}

object UserService {
  def apply[F[_]: Async: Logger]()(
      implicit
      userRepositoryAlgebra: UserRepositoryAlgebra[F],
      passwordHasherWrapper: PasswordHasher[F]
  ): UserService[F] =
    new UserService()
}
