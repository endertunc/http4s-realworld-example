package com.real.world.http4s.repository.algebra

import com.colisweb.tracing.core.TracingContext
import com.real.world.http4s.model._

trait FollowerRepositoryAlgebra[F[_]] {

  def follow(followee: UserId, follower: UserId)(implicit tracingContext: TracingContext[F]): F[Unit]
  def unfollow(followee: UserId, follower: UserId)(implicit tracingContext: TracingContext[F]): F[Unit]
  def isFollowing(followee: UserId, follower: UserId)(implicit tracingContext: TracingContext[F]): F[Boolean]

}
