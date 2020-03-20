package com.real.world.http4s.repository.algebra

import com.real.world.http4s.model.user.User
import com.real.world.http4s.model.user.User.{ Email, UserId, Username }
import com.real.world.http4s.model.profile.Profile
import com.real.world.http4s.model.user.User

import cats.data.NonEmptyList

trait UserRepositoryAlgebra[F[_]] {

  // write
  def insertUser(user: User): F[User]

  // read
  def findUserById(userId: UserId): F[Option[User]]
  def findUserByEmail(email: Email): F[Option[User]]
  def findUserByUsername(username: Username): F[Option[User]]
  def findUsersByUserId(userIds: NonEmptyList[UserId]): F[List[User]]
  def findProfilesByUserId(userIds: NonEmptyList[UserId], follower: UserId): F[Map[UserId, Profile]]

  // update
  def updateUser(user: User): F[Option[User]]

}
