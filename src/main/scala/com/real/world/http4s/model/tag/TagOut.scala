//package com.real.world.http4s.model.tag
//
//import io.circe.Codec
//import io.circe.generic.extras.semiauto.deriveUnwrappedCodec
//
//final case class TagOut(name: String) extends AnyVal
//
//object TagOut {
//  implicit val TagOutCodec: Codec[TagOut] = deriveUnwrappedCodec[TagOut]
//
//  def fromTag(tag: Tag): TagOut = TagOut(tag.name.value)
//}
