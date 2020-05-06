package com.real.world.http4s.repository

import cats.data.NonEmptyList
import cats.effect.IO
import com.real.world.http4s.RealWorldApp
import doobie.scalatest.IOChecker
import com.real.world.http4s.generators.UserGenerator
import com.real.world.http4s.model.user.User
import org.scalatest.flatspec.AnyFlatSpec

class FollowersRepositorySpec extends AnyFlatSpec with IOChecker with RealWorldApp {

  override def transactor: doobie.Transactor[IO] = xa

  val followed: User = UserGenerator.generateUser
  val followee: User = UserGenerator.generateUser

  "FollowersStatements" should "compile" in {
    check(FollowersStatement.follow[IO](followed.id, followee.id).unsafeRunSync())
    check(FollowersStatement.isFollowing(followed.id, followee.id))
    check(FollowersStatement.findFollowees(NonEmptyList.of(followed.id), followee.id))
    check(FollowersStatement.unfollow(followed.id, followee.id))
  }

}
