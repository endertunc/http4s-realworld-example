package com.real.world.http4s.service

import cats.effect.Async
import cats.implicits._

import com.real.world.http4s.model._
import com.real.world.http4s.model.profile.IsFollowing
import com.real.world.http4s.repository.algebra.FollowerRepositoryAlgebra

import io.chrisdavenport.log4cats.Logger

class FollowerService[F[_]: Async: Logger]()(implicit followersRepositoryAlgebra: FollowerRepositoryAlgebra[F]) {

  def isFollowing(userId: UserId, followee: UserId): F[IsFollowing] =
    for {
      _           <- Logger[F].trace(s"Checking if user [$followee] is following user [$userId]")
      isFollowing <- followersRepositoryAlgebra.isFollowing(userId, followee).map(IsFollowing.fromBoolean)
      _           <- Logger[F].trace(s"Currently [$followee] is ${isFollowing} user [$userId]")
    } yield isFollowing

  def follow(followeeId: UserId, follower: UserId): F[Unit] =
    for {
      _ <- Logger[F].trace(s"User [$follower] is following user [$followeeId]")
      _ <- followersRepositoryAlgebra.follow(followeeId, follower)
      _ <- Logger[F].trace(s"User [$follower] successfully followed user [$followeeId]")
    } yield ()

  def unfollow(followeeId: UserId, follower: UserId): F[Unit] =
    for {
      _ <- Logger[F].trace(s"User [$follower] is unfollowing user [$followeeId]")
      _ <- followersRepositoryAlgebra.unfollow(followeeId, follower)
      _ <- Logger[F].trace(s"User [$follower] successfully unfollowed user [$followeeId]")
    } yield ()

}

object FollowerService {
  def apply[F[_]: Async: Logger]()(implicit followersRepositoryAlgebra: FollowerRepositoryAlgebra[F]): FollowerService[F] =
    new FollowerService()
}
