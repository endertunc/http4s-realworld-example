package com.real.world.http4s.http.routes

import org.http4s.HttpRoutes
import cats.effect.Async
import cats.implicits._
import com.colisweb.tracing.core.TracingContext
import com.colisweb.tracing.core.TracingContextBuilder
import com.colisweb.tracing.http.server.TracedHttpRoutes
import com.colisweb.tracing.http.server.TracedHttpRoutes.using
import com.real.world.http4s.http.Http4sAndCirceSupport
import com.real.world.http4s.model.tag.TagResponse
import com.real.world.http4s.service.TagService
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger

class TagRoutes[F[_]: Async: TracingContextBuilder]()(implicit L: SelfAwareStructuredLogger[F], tagService: TagService[F])
    extends Http4sAndCirceSupport[F] {

  val routes: HttpRoutes[F] = TracedHttpRoutes {
    case GET -> Root using implicit0(tracingContext: TracingContext[F]) => tagService.findAll.flatMap(tags => Ok(TagResponse(tags)))
  }

}

object TagRoutes {
  def apply[F[_]: Async: SelfAwareStructuredLogger: TagService: TracingContextBuilder](): TagRoutes[F] = new TagRoutes[F]()
}
