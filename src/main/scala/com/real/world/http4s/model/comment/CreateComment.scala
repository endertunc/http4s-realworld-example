package com.real.world.http4s.model.comment

import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.refined._
import io.circe.{ Decoder, Encoder }

import com.real.world.http4s.model.Instances._
import com.real.world.http4s.model._

final case class CreateComment(body: CommentBody)
final case class CreateCommentWrapper(comment: CreateComment)

object CreateComment {
  implicit val CreateCommentEncoder: Encoder[CreateComment] = deriveEncoder[CreateComment]
  implicit val CreateCommentDecoder: Decoder[CreateComment] = deriveDecoder[CreateComment]
}

object CreateCommentWrapper {
  implicit val CreateCommentWrapperEncoder: Encoder[CreateCommentWrapper] = deriveEncoder[CreateCommentWrapper]
  implicit val CreateCommentWrapperDecoder: Decoder[CreateCommentWrapper] = deriveDecoder[CreateCommentWrapper]
}
