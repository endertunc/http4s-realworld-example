package com.real.world.http4s.service

import cats.effect.Async
import cats.implicits._
import com.colisweb.tracing.core.TracingContext
import com.real.world.http4s.model._
import com.real.world.http4s.model.profile.IsFollowing
import com.real.world.http4s.repository.algebra.FollowerRepositoryAlgebra
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger

class FollowerService[F[_]: Async]()(implicit L: SelfAwareStructuredLogger[F], followersRepositoryAlgebra: FollowerRepositoryAlgebra[F]) {

  def isFollowing(userId: UserId, followee: UserId)(implicit tracingContext: TracingContext[F]): F[IsFollowing] =
    for {
      _           <- L.trace(s"Checking if user [$followee] is following user [$userId]")
      isFollowing <- followersRepositoryAlgebra.isFollowing(userId, followee).map(IsFollowing.fromBoolean)
      _           <- L.trace(s"Currently [$followee] is ${isFollowing} user [$userId]")
    } yield isFollowing

  def follow(followeeId: UserId, follower: UserId)(implicit tracingContext: TracingContext[F]): F[Unit] =
    for {
      _ <- L.trace(s"User [$follower] is following user [$followeeId]")
      _ <- followersRepositoryAlgebra.follow(followeeId, follower)
      _ <- L.trace(s"User [$follower] successfully followed user [$followeeId]")
    } yield ()

  def unfollow(followeeId: UserId, follower: UserId)(implicit tracingContext: TracingContext[F]): F[Unit] =
    for {
      _ <- L.trace(s"User [$follower] is unfollowing user [$followeeId]")
      _ <- followersRepositoryAlgebra.unfollow(followeeId, follower)
      _ <- L.trace(s"User [$follower] successfully unfollowed user [$followeeId]")
    } yield ()

}

object FollowerService {
  def apply[F[_]: Async: SelfAwareStructuredLogger]()(implicit followersRepositoryAlgebra: FollowerRepositoryAlgebra[F]): FollowerService[F] =
    new FollowerService()
}
