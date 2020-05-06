package com.real.world.http4s.service

import cats.data.NonEmptyList
import cats.effect.{ Async, Sync }
import cats.implicits._
import com.colisweb.tracing.core.TracingContext
import com.real.world.http4s.AppError.{ PasswordHashFailed, UserNotFound }
import com.real.world.http4s.authentication.PasswordHasher
import com.real.world.http4s.model._
import com.real.world.http4s.model.profile.{ IsFollowing, Profile }
import com.real.world.http4s.model.user.RegisterUserInput.toUser
import com.real.world.http4s.model.user.{ RegisterUserRequest, UpdateUser, User, UserLoginRequest }
import com.real.world.http4s.repository.algebra.UserRepositoryAlgebra
import io.chrisdavenport.log4cats.{ Logger, SelfAwareStructuredLogger }

class UserService[F[_]: Async]()(
    implicit L: SelfAwareStructuredLogger[F],
    userRepositoryAlgebra: UserRepositoryAlgebra[F],
    passwordHasher: PasswordHasher[F]
) {

  def registerUser(registerUserRequest: RegisterUserRequest)(implicit tracingContext: TracingContext[F]): F[User] =
    for {
      _ <- Logger[F].trace(
        s"Trying to register user new user with email [${registerUserRequest.email}] and username [${registerUserRequest.username}]"
      )
      user      <- registerUserRequest.toUser
      savedUser <- userRepositoryAlgebra.insertUser(user)
      _         <- Logger[F].trace(s"User with email [${registerUserRequest.email}] and username [${registerUserRequest.username}] successfully registered")
    } yield savedUser

  def loginUser(userLoginRequest: UserLoginRequest)(implicit tracingContext: TracingContext[F]): F[User] =
    for {
      _         <- Logger[F].trace(s"User with email [${userLoginRequest.email}] trying to login")
      userAsOpt <- userRepositoryAlgebra.findUserByEmail(userLoginRequest.email)
      user      <- Sync[F].fromOption(userAsOpt, UserNotFound("")) // ToDo msg
      loggedInUser <- Sync[F].ifM(passwordHasher.checkHash(userLoginRequest.password, user.hashedPassword))(
        user.pure[F],
        Logger[F].warn(s"User [${user.id}] failed to login") *>
        PasswordHashFailed("In valid email or password").raiseError[F, User]
      )
      _ <- Logger[F].trace(s"User with email [${userLoginRequest.email}] successfully logged in")
    } yield loggedInUser

  def findUserById(userId: UserId)(implicit tracingContext: TracingContext[F]): F[User] =
    for {
      _         <- Logger[F].trace(s"Trying to find user by id [$userId]")
      userAsOpt <- userRepositoryAlgebra.findUserById(userId)
      user      <- Sync[F].fromOption(userAsOpt, UserNotFound(userId.value.toString))
      _         <- Logger[F].trace(s"Found user with id [$userId]")
    } yield user

  def findUserByUsername(username: Username)(implicit tracingContext: TracingContext[F]): F[User] =
    for {
      _         <- Logger[F].trace(s"Trying to find user by username [$username]")
      userAsOpt <- userRepositoryAlgebra.findUserByUsername(username)
      user      <- Sync[F].fromOption(userAsOpt, UserNotFound("")) // ToDo msg
      _         <- Logger[F].trace(s"Found user with id [${user.id}]")
    } yield user

  def updateUser(updateUser: UpdateUser, userId: UserId)(implicit tracingContext: TracingContext[F]): F[User] =
    for {
      existingUser     <- findUserById(userId)
      user             <- updateUser.toUser[F](existingUser)
      updatedUserAsOpt <- userRepositoryAlgebra.updateUser(user)
      updatedUser      <- Sync[F].fromOption(updatedUserAsOpt, UserNotFound(userId.value.toString))
    } yield updatedUser

  def findProfilesByUserId(usersIds: NonEmptyList[UserId], maybeFollower: Option[UserId])(
      implicit tracingContext: TracingContext[F]
  ): F[Map[UserId, Profile]] =
    maybeFollower.fold(findProfilesByUserId(usersIds))(follower => userRepositoryAlgebra.findProfilesByUserId(usersIds, follower))

  private def findProfilesByUserId(usersIds: NonEmptyList[UserId])(implicit tracingContext: TracingContext[F]): F[Map[UserId, Profile]] =
    userRepositoryAlgebra
      .findUsersByUserId(usersIds)
      .map(users => users.map(user => user.id -> Profile(user, IsFollowing.NotFollowing)).toMap)

}

object UserService {
  def apply[F[_]: Async: SelfAwareStructuredLogger]()(
      implicit userRepositoryAlgebra: UserRepositoryAlgebra[F],
      passwordHasherWrapper: PasswordHasher[F]
  ): UserService[F] =
    new UserService()
}
