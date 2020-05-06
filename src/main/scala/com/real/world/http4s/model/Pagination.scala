package com.real.world.http4s.model

// I was too lazy to make sure limit and offsets are valid...
// ToDo don't be lazy and switch to proper pagination with validation etc.
final case class Pagination(limit: Long, offset: Long)
