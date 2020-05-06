package com.real.world.http4s.service

import com.real.world.http4s.RealWorldApp
import com.real.world.http4s.generators.UserGenerator
import com.real.world.http4s.model.profile.IsFollowing.{ Following, NotFollowing }
import com.real.world.http4s.quill.Followers
import org.scalatest.flatspec.AsyncFlatSpec

class FollowerServiceSpec extends AsyncFlatSpec with RealWorldApp {

  "FollowerService" should "allow users to follow other users" in IOSuit {
    for {
      persistedUser     <- insertUser()
      persistedFollowee <- insertUser()
      _                 <- ctx.followerService.follow(persistedUser.id, persistedFollowee.id)
      record            <- Followers.findFollowersRecord(persistedUser.id, persistedFollowee.id)
    } yield record shouldBe defined
  }

  it should "allow users to unfollow other users" in IOSuit {
    for {
      persistedUser     <- insertUser()
      persistedFollowee <- insertUser()
      _                 <- ctx.followerService.follow(persistedUser.id, persistedFollowee.id)
      _                 <- Followers.findFollowersRecord(persistedUser.id, persistedFollowee.id).map(_.get)
      _                 <- ctx.followerService.unfollow(persistedUser.id, persistedFollowee.id)
      record            <- Followers.findFollowersRecord(persistedUser.id, persistedFollowee.id)
    } yield record should not be defined
  }

  it should "returns true when user is following the other user" in IOSuit {
    for {
      persistedUser     <- insertUser()
      persistedFollowee <- insertUser()
      _                 <- ctx.followerService.follow(persistedUser.id, persistedFollowee.id)
      isFollowing       <- ctx.followerService.isFollowing(persistedUser.id, persistedFollowee.id)
    } yield isFollowing shouldBe Following
  }

  it should "returns false when user is not following the other user" in IOSuit {
    for {
      persistedUser     <- insertUser()
      persistedFollowee <- insertUser()
      isFollowing       <- ctx.followerService.isFollowing(persistedUser.id, persistedFollowee.id)
    } yield isFollowing shouldBe NotFollowing
  }

  it should "not allow to follow non-existing users" in IOSuit {
    for {
      persistedFollower <- insertUser()
      unpersistedFollowee = UserGenerator.generateUser()
      isFollowing <- ctx.followerService.isFollowing(unpersistedFollowee.id, persistedFollower.id)
    } yield isFollowing shouldBe NotFollowing
  }

  it should "not report failure if user is not followed on unfollow" ignore {
    ???
  }

  it should "failed to follow if you already fallowed the same user before" ignore {
    ???
  }

}
