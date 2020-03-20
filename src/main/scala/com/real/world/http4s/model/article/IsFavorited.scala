package com.real.world.http4s.model.article

import io.circe.Codec
import io.circe.generic.extras.semiauto.deriveUnwrappedCodec

final case class IsFavorited(value: Boolean) extends AnyVal

object IsFavorited {

  final val NotFavorited = IsFavorited(false)
  final val Favorited    = IsFavorited(true)

  implicit val IsFavoritedCodec: Codec[IsFavorited] = deriveUnwrappedCodec[IsFavorited]
}
