package com.real.world.http4s.repository

import java.sql.SQLException
import java.time.Instant

import com.real.world.http4s.AppError.UserAlreadyExist
import com.real.world.http4s.model.profile.{ IsFollowing, Profile }
import com.real.world.http4s.model.user.User
import com.real.world.http4s.model.user.User.{ Email, UserId, Username }
import com.real.world.http4s.repository.algebra.UserRepositoryAlgebra
import com.real.world.http4s.model.profile.{ IsFollowing, Profile }
import com.real.world.http4s.model.user.User
import com.real.world.http4s.repository.algebra.UserRepositoryAlgebra

import cats.data.NonEmptyList
import cats.implicits._
import cats.effect.Async

import doobie._
import doobie.`enum`.SqlState
import doobie.implicits._
import doobie.implicits.legacy.instant.JavaTimeInstantMeta
import doobie.postgres._
import io.chrisdavenport.log4cats.Logger

class PostgresUserRepositoryAlgebra[F[_]: Async: Logger]()(implicit xa: Transactor[F]) extends UserRepositoryAlgebra[F] {

  // This just one way to handle existing user case.
  // When you have more complex logic/requirements you might want to move checking existing user responsibility to service layer
  override def insertUser(user: User): F[User] =
    for {
      query <- UserStatement.saveUser[F](user)
      user <- query.unique
        .transact(xa)
        .recoverWith {
          case e: SQLException if SqlState(e.getSQLState) == sqlstate.class23.UNIQUE_VIOLATION =>
            Logger[F].error(e)(s"User with email [${user.email}] or username [${user.username}] is ealready exist") *>
            UserAlreadyExist(s"User with email [${user.email}] or username [${user.username}] is ealready exist").raiseError[F, User]
        }
    } yield user

  override def findUserByEmail(email: Email): F[Option[User]] =
    UserStatement.findUserByEmail(email).option.transact(xa)

  override def updateUser(user: User): F[Option[User]] = UserStatement.updateUser[F](user) >>= (_.option.transact(xa))

  override def findUserByUsername(username: Username): F[Option[User]] =
    UserStatement.findUserByUsername(username).option.transact(xa)

  override def findUserById(userId: UserId): F[Option[User]] =
    UserStatement.findUserByUserId(userId).option.transact(xa)

  override def findProfilesByUserId(userIds: NonEmptyList[UserId], follower: UserId): F[Map[UserId, Profile]] =
    UserStatement
      .findProfilesByUserId(userIds, follower)
      .to[List]
      .map {
        _.map {
          case (user, isFollowing) => user.id -> Profile(user, IsFollowing.fromBoolean(isFollowing.isDefined))
        }.toMap
      }
      .transact(xa)

  override def findUsersByUserId(userIds: NonEmptyList[UserId]): F[List[User]] =
    UserStatement.findUsersByUserId(userIds).to[List].transact(xa)
}

object PostgresUserRepositoryAlgebra {
  def apply[F[_]: Async: Logger: Transactor](): UserRepositoryAlgebra[F] = new PostgresUserRepositoryAlgebra()
}

object UserStatement {

  def saveUser[F[_]: Async](user: User): F[doobie.Query0[User]] =
    Async[F].delay(Instant.now).map { now =>
      sql"""
        INSERT INTO users (email, password, username, created_at, updated_at)
        VALUES (${user.email}, ${user.hashedPassword}, ${user.username}, $now, $now)
        RETURNING id, email, password, username, bio, image""".query[User]
    }

  def findUserByEmail(email: Email): doobie.Query0[User] =
    sql"SELECT id, email, password, username, bio, image FROM users WHERE email=$email".query[User]

  def updateUser[F[_]: Async](user: User): F[doobie.Query0[User]] =
    Async[F].delay(Instant.now).map { now =>
      sql"""
         UPDATE users SET
         (email, password, username, bio, image, updated_at)
         =
         (${user.email}, ${user.hashedPassword}, ${user.username}, ${user.bio}, ${user.image}, $now)
         WHERE id=${user.id}
         RETURNING id, email, password, username, bio, image""".query[User]
    }

  def findUserByUsername(username: Username): doobie.Query0[User] =
    sql"SELECT id, email, password, username, bio, image FROM users WHERE username=$username".query[User]

  def findProfileByUsername(username: Username): doobie.Query0[(User, Boolean)] =
    sql"""SELECT users.id, users.email, users.password, users.username, users.bio, users.image,
          followers.followee_id IS NOT NULL AS isFollowing FROM users
          LEFT JOIN followers ON followers.followee_id = users.id
          WHERE users.username=$username""".query[(User, Boolean)]

  def findProfileByUserId(userId: UserId): doobie.Query0[(User, Boolean)] =
    sql"""SELECT users.id, users.email, users.password, users.username, users.bio, users.image,
          followers.followee_id IS NOT NULL AS isFollowing FROM users
          LEFT JOIN followers ON followers.followee_id = users.id
          WHERE users.id=$userId""".query[(User, Boolean)]

  // ToDo double check this function. It works buuuut
  def findProfilesByUserId(userIds: NonEmptyList[UserId], follower: UserId): doobie.Query0[(User, Option[Int])] =
    (fr"""SELECT users.id, users.email, users.password, users.username, users.bio, users.image,
          followers.follower_id FROM users
          LEFT JOIN followers ON followers.followee_id = users.id
          WHERE (followers.follower_id = $follower OR followers.follower_id IS NULL) AND """ ++ Fragments.in(fr"users.id", userIds))
      .query[(User, Option[Int])]

  def findUsersByUserId(userIds: NonEmptyList[UserId]): doobie.Query0[User] =
    (fr"""SELECT id, email, password, username, bio, image FROM users WHERE """ ++ Fragments.in(fr"id", userIds))
      .query[User]

  def findUserByUserId(userId: UserId): doobie.Query0[User] =
    sql"SELECT id, email, password, username, bio, image FROM users WHERE id=$userId".query[User]

}
