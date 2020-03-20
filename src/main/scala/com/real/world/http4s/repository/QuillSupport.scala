package com.real.world.http4s.repository

import java.time.Instant
import java.util.Date

import doobie.quill.DoobieContext

import io.getquill.Literal

trait QuillSupport {
  val DoobiePostgresContext = new DoobieContext.Postgres(Literal)
  import DoobiePostgresContext._

  implicit val instanceEncoder: MappedEncoding[Instant, Date] = MappedEncoding[Instant, Date](Date.from)
  implicit val instantDecoder: MappedEncoding[Date, Instant]  = MappedEncoding[Date, Instant](_.toInstant)
}

object QuillSupport extends QuillSupport
