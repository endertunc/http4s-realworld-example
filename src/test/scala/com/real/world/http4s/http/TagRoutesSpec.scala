package com.real.world.http4s.http

import org.http4s.circe.{ CirceEntityDecoder, CirceEntityEncoder }
import org.http4s.implicits._
import org.http4s.{ Request, Method }

import cats.effect.IO

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

import com.real.world.http4s.base.ServicesAndRepos
import com.real.world.http4s.generators.TagGenerator
import com.real.world.http4s.model.tag.TagResponse

import org.scalatest.flatspec.AsyncFlatSpec

class TagRoutesSpec extends AsyncFlatSpec with ServicesAndRepos with CirceEntityDecoder with CirceEntityEncoder {

  implicit val TagResponseDecoder: Decoder[TagResponse] = deriveDecoder[TagResponse]

  "Tag routes" should "list all tags" in IOSuit {
    val tagIn1 = TagGenerator.generateTagIn()
    val tagIn2 = TagGenerator.generateTagIn()
    val tagIns = List(tagIn1, tagIn2)
    val request = Request[IO](
      method = Method.GET,
      uri    = uri"/api/tags"
    )
    for {
      _           <- ctx.tagService.createTags(tagIns)
      response    <- ctx.httpApp.run(request)
      tagResponse <- response.as[TagResponse]
    } yield tagResponse.tags.filter(t => tagIns.exists(p => p.name == t.name)) should have size 2

  }

}
