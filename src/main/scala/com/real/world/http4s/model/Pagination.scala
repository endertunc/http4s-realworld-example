package com.real.world.http4s.model

// I was too lazy to make sure limit and offsets are valid...
final case class Pagination(limit: Long, offset: Long)
