package com.real.world.http4s.service

import com.real.world.http4s.model.user.User.{ UserId, Username }
import com.real.world.http4s.model.profile.Profile

import cats.effect.Async
import cats.implicits._
import io.chrisdavenport.log4cats.Logger

class ProfileService[F[_]: Async: Logger]()(implicit userService: UserService[F], followersService: FollowerService[F]) {

  def findProfileByUsername(profileUsername: Username, followerId: UserId): F[Profile] =
    for {
      _            <- Logger[F].trace(s"Finding profile by username [${profileUsername}], followerId: [${followerId}]")
      followeeUser <- userService.findUserByUsername(profileUsername)
      isFollowing  <- followersService.isFollowing(followeeUser.id, followerId)
    } yield new Profile(
      username  = followeeUser.username,
      bio       = followeeUser.bio,
      image     = followeeUser.image,
      following = isFollowing
    )

  def findProfileByUserId(userId: UserId, followerId: UserId): F[Profile] =
    for {
      _            <- Logger[F].trace(s"Finding profile by user [$userId] and followerId: [$followerId]")
      followeeUser <- userService.findUserById(userId)
      isFollowing  <- followersService.isFollowing(followeeUser.id, followerId)
    } yield Profile(
      username  = followeeUser.username,
      bio       = followeeUser.bio,
      image     = followeeUser.image,
      following = isFollowing
    )

  def follow(followeeUsername: Username, followerId: UserId): F[Unit] =
    for {
      _            <- Logger[F].trace(s"User [$followerId] is trying to follow user [$followeeUsername]")
      followee     <- userService.findUserByUsername(followeeUsername)
      followResult <- followersService.follow(followee.id, followerId)
      _            <- Logger[F].trace(s"User [$followerId] successfully followed user [${followee.id}]")
    } yield followResult

  def unfollow(followeeUsername: Username, followerId: UserId): F[Unit] =
    for {
      _              <- Logger[F].trace(s"User [$followerId] is trying to unfollow user [$followeeUsername]")
      followee       <- userService.findUserByUsername(followeeUsername)
      unfollowResult <- followersService.unfollow(followee.id, followerId)
      _              <- Logger[F].trace(s"User [$followerId] successfully unfollowed user [${followee.id}]")
    } yield unfollowResult

}

object ProfileService {

  def apply[F[_]: Async: Logger]()(implicit userService: UserService[F], followersService: FollowerService[F]): ProfileService[F] =
    new ProfileService()

}
