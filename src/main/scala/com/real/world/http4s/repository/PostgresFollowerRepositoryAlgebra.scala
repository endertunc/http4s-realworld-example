package com.real.world.http4s.repository

import java.sql.SQLException
import java.time.Instant

import cats.data.NonEmptyList
import cats.effect.Async
import cats.implicits._

import doobie._
import doobie.`enum`.SqlState
import doobie.implicits._
import doobie.implicits.legacy.instant.JavaTimeInstantMeta
import doobie.postgres.sqlstate
import doobie.refined.implicits._

import com.real.world.http4s.AppError.{ FolloweeNotFound, RecordNotFound }
import com.real.world.http4s.model.Instances._
import com.real.world.http4s.model._
import com.real.world.http4s.repository.algebra.FollowerRepositoryAlgebra

import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.chrisdavenport.log4cats.{ Logger, SelfAwareStructuredLogger }

class PostgresFollowerRepositoryAlgebra[F[_]: Async: Logger]()(implicit xa: Transactor[F]) extends FollowerRepositoryAlgebra[F] {

  implicit val logger: SelfAwareStructuredLogger[F] = Slf4jLogger.getLogger[F]

  override def isFollowing(followee: UserId, follower: UserId): F[Boolean] =
    FollowersStatement
      .isFollowing(followee, follower)
      .option
      .map(_.isDefined)
      .transact(xa)

  // ToDo map(_ => ())
  override def follow(followee: UserId, follower: UserId): F[Unit] =
    for {
      query <- FollowersStatement.follow[F](followee, follower)
      _ <- query.unique
        .map(_ => ())
        .transact(xa)
        .recoverWith {
          case e: SQLException if SqlState(e.getSQLState) == sqlstate.class23.FOREIGN_KEY_VIOLATION =>
            // ToDo there is actually an edge case which makes this message invalid
            Logger[F].error(e)(s"Followee [$followee] or follower [$follower] does not exist anymore") *>
            FolloweeNotFound(s"The user you are trying to follow does not exist").raiseError[F, Unit]
        }
    } yield ()

  override def unfollow(followee: UserId, follower: UserId): F[Unit] =
    FollowersStatement
      .unfollow(followee, follower)
      .run
      .transact(xa)
      .flatMap {
        case effectedRows: Int if effectedRows == 0 =>
          Logger[F].warn(s"No relation is found between followee [$followee] and follower [$follower]") *> RecordNotFound(
            s"User is already unfollowed"
          ).raiseError[F, Unit]
        case effectedRows: Int if effectedRows == 1 => ().pure[F]
      }

}

object PostgresFollowerRepositoryAlgebra {
  def apply[F[_]: Async: Logger: Transactor](): FollowerRepositoryAlgebra[F] = new PostgresFollowerRepositoryAlgebra()
}

object FollowersStatement {

  def isFollowing(followee: UserId, follower: UserId): doobie.Query0[(Int, Int)] =
    sql"SELECT followee_id, follower_id FROM followers WHERE followee_id=$followee AND follower_id=$follower".query[(Int, Int)]

  def findFollowees(followees: NonEmptyList[UserId], follower: UserId): doobie.Query0[UserId] =
    (fr"SELECT followee_id FROM followers WHERE follower_id=$follower AND " ++ Fragments.in(fr"followee_id", followees)).query[UserId]

  def follow[F[_]: Async](followee: UserId, follower: UserId): F[doobie.Query0[(Int, Int)]] =
    Async[F].delay(Instant.now).map { now =>
      sql"""
          INSERT INTO followers (followee_id, follower_id, inserted_at) VALUES ($followee, $follower, $now)
          RETURNING followee_id, follower_id""".query[(Int, Int)]
    }

  def unfollow(followee: UserId, follower: UserId): doobie.Update0 =
    sql"DELETE FROM followers WHERE followee_id=$followee AND follower_id=$follower".update

}
