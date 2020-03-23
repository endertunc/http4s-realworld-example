package com.real.world.http4s.model.comment

import java.time.Instant

import io.circe.generic.semiauto._
import io.circe.refined._
import io.circe.refined._
import io.circe.{ Encoder, Decoder }

import doobie.refined.implicits._
import doobie.util.meta.Meta

import com.real.world.http4s.model.Instances._
import com.real.world.http4s.model._
import com.real.world.http4s.model._
import com.real.world.http4s.model.comment.Comment.CommentId

import eu.timepit.refined.types.numeric.PosInt
import io.estatico.newtype.macros.newtype
import scalaz.annotation.deriving
final case class Comment(id: CommentId, body: CommentBody, articleId: ArticleId, authorId: UserId, createdAt: Instant, updatedAt: Instant)

object Comment {

  @deriving(Meta, Decoder, Encoder) @newtype case class CommentId(value: PosInt)

  implicit val CommentEncoder: Encoder[Comment] = deriveEncoder[Comment]
  implicit val CommentDecoder: Decoder[Comment] = deriveDecoder[Comment]
}
