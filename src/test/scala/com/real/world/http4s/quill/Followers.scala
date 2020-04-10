package com.real.world.http4s.quill

import java.time.Instant

import cats.effect.IO

import doobie.syntax.connectionio.toConnectionIOOps

import com.real.world.http4s.model.UserId
import com.real.world.http4s.quill.DoobiePostgresContext._
import com.real.world.http4s.repository.QuillSupport

import io.getquill.{ idiom => _ }

object Followers extends QuillSupport {

  case class Followers(followee: UserId, follower: UserId, insertedAt: Instant)

  private val followers = quote {
    querySchema[Followers](
      "followers",
      _.followee -> "followee_id",
      _.follower -> "follower_id",
      _.insertedAt -> "inserted_at"
    )
  }

  def findFollowersRecord(followee: UserId, follower: UserId)(implicit xa: doobie.Transactor[IO]): IO[Option[Followers]] =
    run(quote(followers.filter(record => record.followee == lift(followee) && record.follower == lift(follower))))
      .map(_.headOption)
      .transact(xa)

  def findFollowersForUser(userId: UserId, followee: UserId)(implicit xa: doobie.Transactor[IO]): IO[List[Followers]] =
    run(quote(followers.filter(_.followee == lift(userId)))).transact(xa)

}
