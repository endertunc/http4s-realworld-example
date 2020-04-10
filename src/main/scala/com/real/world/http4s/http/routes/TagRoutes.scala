package com.real.world.http4s.http.routes

import org.http4s.HttpRoutes

import cats.effect.Async
import cats.implicits._

import com.colisweb.tracing.core.TracingContextBuilder
import com.colisweb.tracing.http.server.TracedHttpRoutes
import com.colisweb.tracing.http.server.TracedHttpRoutes.using

import com.real.world.http4s.http.BaseHttp4s
import com.real.world.http4s.model.tag.TagResponse
import com.real.world.http4s.service.TagService

import io.chrisdavenport.log4cats.Logger

class TagRoutes[F[_]: Async: Logger: TracingContextBuilder]()(implicit tagService: TagService[F]) extends BaseHttp4s[F] {

  val routes: HttpRoutes[F] = TracedHttpRoutes {
    case GET -> Root using tracingContext => tagService.findAll.flatMap(tags => Ok(TagResponse(tags)))
  }

}

object TagRoutes {
  def apply[F[_]: Async: Logger: TagService: TracingContextBuilder](): TagRoutes[F] = new TagRoutes[F]()
}
