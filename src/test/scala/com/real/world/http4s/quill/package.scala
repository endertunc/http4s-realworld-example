package com.real.world.http4s


import doobie.quill.DoobieContext

import io.getquill.Literal

package object quill {


  val DoobiePostgresContext = new DoobieContext.Postgres(Literal)

}
