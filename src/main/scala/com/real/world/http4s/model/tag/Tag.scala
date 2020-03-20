package com.real.world.http4s.model.tag

import Tag.{ TagId, TagName }

final case class Tag(id: TagId, name: TagName)

object Tag {
  case class TagId(id: Int) extends AnyVal
  case class TagName(value: String) extends AnyVal
}
