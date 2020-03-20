package com.real.world.http4s.repository.algebra

import com.real.world.http4s.model.user.User.UserId

trait FollowerRepositoryAlgebra[F[_]] {

  def follow(followee: UserId, follower: UserId): F[Unit]
  def unfollow(followee: UserId, follower: UserId): F[Unit]
  def isFollowing(followee: UserId, follower: UserId): F[Boolean]

}
