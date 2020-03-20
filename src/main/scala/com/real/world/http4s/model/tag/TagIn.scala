package com.real.world.http4s.model.tag

import io.circe.Codec
import io.circe.generic.extras.semiauto.deriveUnwrappedCodec

final case class TagIn(name: String) extends AnyVal

object TagIn {
  // ToDo Codec???
  implicit val TagInCodec: Codec[TagIn] = deriveUnwrappedCodec[TagIn]
}
