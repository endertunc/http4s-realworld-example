package com.real.world.http4s.generators

import com.real.world.http4s.model.tag.Tag.TagName

import org.scalacheck.Gen

object TagGenerator extends GeneratorsBase {

  def generateTagIn(): TagName = tagNameGen.sample.get
  def generateTagInList(size: Int = 5): List[TagName] = Gen.listOfN(size, tagNameGen).sample.get // scalastyle:off magic.number

}
