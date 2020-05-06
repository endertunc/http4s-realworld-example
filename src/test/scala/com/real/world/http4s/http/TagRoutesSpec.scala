package com.real.world.http4s.http

import org.http4s.circe.{ CirceEntityDecoder, CirceEntityEncoder }
import org.http4s.implicits._
import org.http4s.{ Method, Request }
import cats.effect.IO
import com.real.world.http4s.RealWorldApp
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.refined._
import com.real.world.http4s.generators.TagGenerator
import com.real.world.http4s.model.NewTypeImplicits._
import com.real.world.http4s.model.tag.TagResponse
import org.scalatest.flatspec.AsyncFlatSpec

class TagRoutesSpec extends AsyncFlatSpec with RealWorldApp with CirceEntityDecoder with CirceEntityEncoder {

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
    } yield tagResponse.tags should have size 2
//    } yield tagResponse.tags.filter(t => tagIns.exists(p => p.value.value == t.value.value)) should have size 2

  }

}
