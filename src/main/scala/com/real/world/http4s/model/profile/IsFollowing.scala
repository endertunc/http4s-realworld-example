package com.real.world.http4s.model.profile

import io.circe.Codec
import io.circe.generic.extras.semiauto.deriveUnwrappedCodec

final case class IsFollowing(value: Boolean) extends AnyVal

object IsFollowing {
  final val NotFollowing = IsFollowing(false)
  final val Following    = IsFollowing(true)

  implicit val IsFollowingCodec: Codec[IsFollowing] = deriveUnwrappedCodec[IsFollowing]

  def fromBoolean(boolean: Boolean): IsFollowing = if (boolean) Following else NotFollowing
}
