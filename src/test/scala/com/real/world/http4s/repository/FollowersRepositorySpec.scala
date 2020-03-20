package com.real.world.http4s.repository

import com.real.world.http4s.base.ServicesAndRepos
import com.real.world.http4s.generators.UserGenerator
import com.real.world.http4s.model.user.User
import org.scalatest.flatspec.AnyFlatSpec

import cats.data.NonEmptyList
import cats.effect.IO

import doobie.scalatest.IOChecker

class FollowersRepositorySpec extends AnyFlatSpec with IOChecker with ServicesAndRepos {

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
