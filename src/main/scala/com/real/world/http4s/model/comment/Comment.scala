package com.real.world.http4s.model.comment

import java.time.Instant

import io.circe.generic.semiauto._
import io.circe.refined._
import io.circe.{ Decoder, Encoder }

import doobie.refined.implicits._
import doobie.util.meta.Meta

import com.real.world.http4s.model.NewTypeImplicits._
import com.real.world.http4s.model._
import com.real.world.http4s.model.comment.Comment.CommentId

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import io.estatico.newtype.macros.newtype
import scalaz.annotation.deriving

final case class Comment(id: CommentId, body: CommentBody, articleId: ArticleId, authorId: UserId, createdAt: Instant, updatedAt: Instant)

object Comment {

  // SCoverage does not work well with newtype
  // $COVERAGE-OFF$
  type CommentIdAux = NonNegative
  @deriving(Meta, Decoder, Encoder) @newtype case class CommentId(value: Int Refined CommentIdAux)
  object CommentId {
    def from(v: Int): Either[String, CommentId] = refineV[CommentIdAux](v).map(CommentId(_))
    def unsafeFrom(v: Int): CommentId           = CommentId(refineV[CommentIdAux].unsafeFrom(v))
  }
  // $COVERAGE-ON$

  implicit val CommentEncoder: Encoder[Comment] = deriveEncoder[Comment]
  implicit val CommentDecoder: Decoder[Comment] = deriveDecoder[Comment]
}
