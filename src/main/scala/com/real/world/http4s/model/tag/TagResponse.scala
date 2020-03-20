package com.real.world.http4s.model.tag

import io.circe.generic.semiauto.deriveEncoder
import io.circe.Encoder

final case class TagResponse(tags: List[TagOut])

object TagResponse {
  implicit val TagResponseEncoder: Encoder[TagResponse] = deriveEncoder[TagResponse]

  def apply(tags: => List[Tag]): TagResponse = TagResponse(tags.map(tag => TagOut(tag.name.value)))
}
