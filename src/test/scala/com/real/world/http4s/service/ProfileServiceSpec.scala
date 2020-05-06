package com.real.world.http4s.service

import com.real.world.http4s.AppError.{ FolloweeNotFound, RecordNotFound, UserNotFound }
import com.real.world.http4s.RealWorldApp
import com.real.world.http4s.generators.UserGenerator
import com.real.world.http4s.model.profile.IsFollowing.{ Following, NotFollowing }
import com.real.world.http4s.quill.Followers
import org.scalatest.flatspec.AsyncFlatSpec

class ProfileServiceSpec extends AsyncFlatSpec with RealWorldApp {

  "Profile Service" should "allow to follow other users by username" in IOSuit {
    for {
      persistedUser     <- insertUser()
      persistedFollowee <- insertUser()
      _                 <- ctx.profileService.follow(persistedUser.username, persistedFollowee.id)
      record            <- Followers.findFollowersRecord(persistedUser.id, persistedFollowee.id)
    } yield record shouldBe defined
  }

  it should "allow users to unfollow other users by username" in IOSuit {
    for {
      persistedUser     <- insertUser()
      persistedFollowee <- insertUser()
      _                 <- ctx.profileService.follow(persistedUser.username, persistedFollowee.id)
      _                 <- Followers.findFollowersRecord(persistedUser.id, persistedFollowee.id).map(_.get)
      _                 <- ctx.profileService.unfollow(persistedUser.username, persistedFollowee.id)
      record            <- Followers.findFollowersRecord(persistedUser.id, persistedFollowee.id)
    } yield record should not be defined
  }

  it should "fail with FolloweeNotFound when follower does not exist" in FailedIOSuit {
    val unpersistedFollower = UserGenerator.generateUser()
    for {
      persistedFollowee <- insertUser()
      _                 <- ctx.profileService.follow(persistedFollowee.username, unpersistedFollower.id)
    } yield fail("Followed a non-existing user")
  }(_ shouldBe a[FolloweeNotFound])

  it should "fail with UserNotFound when user to follow does not exist" in FailedIOSuit {
    val unpersistedFollowee = UserGenerator.generateUser()
    for {
      persistedFollower <- insertUser()
      _                 <- ctx.profileService.follow(unpersistedFollowee.username, persistedFollower.id)
    } yield fail("Followed a non-existing user")
  }(_ shouldBe a[UserNotFound])

  it should "fail with FolloweeNotFound when unfollow unfollowed user" in FailedIOSuit {
    val unpersistedFollower = UserGenerator.generateUser()
    for {
      persistedUser <- insertUser()
      _             <- ctx.profileService.unfollow(persistedUser.username, unpersistedFollower.id)
    } yield fail("Unfollowed an unfollwed user")
  }(_ shouldBe a[RecordNotFound])

  it should "fail with FolloweeNotFound when user is already unfollowed" in FailedIOSuit {
    for {
      persistedUser     <- insertUser()
      persistedFollowee <- insertUser()
      _                 <- ctx.profileService.unfollow(persistedUser.username, persistedFollowee.id)
      _                 <- ctx.profileService.unfollow(persistedUser.username, persistedFollowee.id)
    } yield fail("Followed an already followed user")
  }(_ shouldBe a[RecordNotFound])

  it should "retrieve profile for followed user" in IOSuit {
    for {
      persistedUser     <- insertUser()
      persistedFollowee <- insertUser()
      profileBefore     <- ctx.profileService.findProfileByUsername(persistedUser.username, persistedFollowee.id)
      _ = profileBefore.following shouldBe NotFollowing
      _            <- ctx.profileService.follow(persistedUser.username, persistedFollowee.id)
      profileAfter <- ctx.profileService.findProfileByUsername(persistedUser.username, persistedFollowee.id)
    } yield {
      profileAfter.username shouldBe persistedUser.username
      profileAfter.following shouldBe Following
    }
  }

  it should "retrieve profile for unfollowed user" in IOSuit {
    for {
      persistedUser     <- insertUser()
      persistedFollowee <- insertUser()
      profile           <- ctx.profileService.findProfileByUsername(persistedUser.username, persistedFollowee.id)

    } yield {
      profile.username shouldBe persistedUser.username
      profile.following shouldBe NotFollowing
    }
  }

}
