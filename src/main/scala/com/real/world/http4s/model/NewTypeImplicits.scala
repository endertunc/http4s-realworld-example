package com.real.world.http4s.model

import io.circe.{ Decoder, Encoder }

import doobie.Meta

import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._

object NewTypeImplicits {

  implicit def coercibleDecoder[A: Coercible[B, *], B: Decoder]: Decoder[A] =
    Decoder[B].map(_.coerce[A])

  implicit def coercibleEncoder[A: Coercible[B, *], B: Encoder]: Encoder[A] =
    Encoder[B].contramap(_.repr.asInstanceOf[B])

  implicit def newTypeDoobieMeta[R, N](implicit ev: Coercible[Meta[R], Meta[N]], R: Meta[R]): Meta[N] =
    ev(R)

}
