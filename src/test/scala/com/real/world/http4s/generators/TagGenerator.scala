package com.real.world.http4s.generators

import com.real.world.http4s.model.tag.TagIn
import org.scalacheck.Gen

object TagGenerator {

  private def tagInGen: Gen[TagIn] = Gen.alphaNumStr.map(TagIn(_))

  def generateTagIn(): TagIn = tagInGen.sample.get
  def generateTagInList(size: Int = 5): List[TagIn] = Gen.listOfN(size, tagInGen).sample.get

}
