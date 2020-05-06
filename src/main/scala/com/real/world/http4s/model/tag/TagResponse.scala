package com.real.world.http4s.model.tag

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import io.circe.refined._

import com.real.world.http4s.model.NewTypeImplicits._
import com.real.world.http4s.model.tag.Tag.TagName

final case class TagResponse(tags: List[TagName])

object TagResponse {
  implicit val TagResponseEncoder: Encoder[TagResponse] = deriveEncoder[TagResponse]

  def apply(tags: => List[Tag]): TagResponse = TagResponse(tags.map(_.name))
}
