package com.real.world.http4s.http.routes

import com.real.world.http4s.http.BaseHttp4s
import com.real.world.http4s.model.tag.TagResponse
import com.real.world.http4s.service.TagService
import com.real.world.http4s.http.BaseHttp4s
import com.real.world.http4s.model.tag.TagResponse
import com.real.world.http4s.service.TagService

import cats.effect.Async
import cats.implicits._

import org.http4s.HttpRoutes
import io.chrisdavenport.log4cats.Logger

class TagRoutes[F[_]: Async: Logger]()(implicit tagService: TagService[F]) extends BaseHttp4s {

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => tagService.findAll.map(TagResponse(_)).toResponse
  }

}

object TagRoutes {
  def apply[F[_]: Async: Logger: TagService](): TagRoutes[F] = new TagRoutes[F]()
}
