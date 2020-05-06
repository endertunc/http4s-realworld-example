package com.real.world.http4s.repository.algebra

import cats.data.NonEmptyList
import com.colisweb.tracing.core.TracingContext
import com.real.world.http4s.model._
import com.real.world.http4s.model.profile.Profile
import com.real.world.http4s.model.user.User

trait UserRepositoryAlgebra[F[_]] {

  // write
  def insertUser(user: User)(implicit tracingContext: TracingContext[F]): F[User]

  // read
  def findUserById(userId: UserId)(implicit tracingContext: TracingContext[F]): F[Option[User]]
  def findUserByEmail(email: Email)(implicit tracingContext: TracingContext[F]): F[Option[User]]
  def findUserByUsername(username: Username)(implicit tracingContext: TracingContext[F]): F[Option[User]]
  def findUsersByUserId(userIds: NonEmptyList[UserId])(implicit tracingContext: TracingContext[F]): F[List[User]]
  def findProfilesByUserId(userIds: NonEmptyList[UserId], follower: UserId)(implicit tracingContext: TracingContext[F]): F[Map[UserId, Profile]]

  // update
  def updateUser(user: User)(implicit tracingContext: TracingContext[F]): F[Option[User]]

}
