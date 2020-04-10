package com.real.world.http4s.http

import org.http4s.circe.{ CirceEntityDecoder, CirceEntityEncoder }
import org.http4s.dsl.Http4sDsl

trait BaseHttp4s[F[_]] extends Http4sDsl[F] with CirceEntityEncoder with CirceEntityDecoder
