package com.real.world.http4s.model.tag

import Tag.{ TagId, TagName }
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import io.estatico.newtype.macros.newtype

final case class Tag(id: TagId, name: TagName)

object Tag {
  // SCoverage does not work well with newtype
  // $COVERAGE-OFF$
  type TagIdAux = NonNegative
  @newtype case class TagId(value: Int Refined TagIdAux)
  object TagId {
    def from(v: Int): Either[String, TagId] = refineV[TagIdAux](v).map(TagId(_))
    def unsafeFrom(v: Int): TagId           = TagId(refineV[TagIdAux].unsafeFrom(v))
  }

  type TagNameAux = NonEmpty
  @newtype case class TagName(value: String Refined TagNameAux)
  object TagName {
    def from(v: String): Either[String, TagName] = refineV[TagNameAux](v).map(TagName(_))
    def unsafeFrom(v: String): TagName           = TagName(refineV[TagNameAux].unsafeFrom(v))
  }
  // $COVERAGE-ON$

}
