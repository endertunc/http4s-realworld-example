package com.real.world.http4s.model.comment
import java.time.Instant

import io.circe.generic.semiauto._
import io.circe.refined._
import io.circe.{ Decoder, Encoder }

import com.real.world.http4s.model.Instances._
import com.real.world.http4s.model._
import com.real.world.http4s.model.comment.Comment.CommentId
import com.real.world.http4s.model.profile.Profile

final case class CommentResponse(id: CommentId, createdAt: Instant, updatedAt: Instant, body: CommentBody, author: Profile)
final case class CommentResponseWrapper(comment: CommentResponse)
final case class CommentListResponseOutWrapper(comments: List[CommentResponse])

object CommentResponse {
  implicit val CommentResponseEncoder: Encoder[CommentResponse] = deriveEncoder[CommentResponse]
  implicit val CommentResponseDecoder: Decoder[CommentResponse] = deriveDecoder[CommentResponse]

  def apply(comment: Comment, author: Profile): CommentResponse =
    CommentResponse(comment.id, comment.createdAt, comment.updatedAt, comment.body, author)
}

object CommentResponseWrapper {
  implicit val CommentResponseWrapperEncoder: Encoder[CommentResponseWrapper] = deriveEncoder[CommentResponseWrapper]
  implicit val CommentResponseWrapperDecoder: Decoder[CommentResponseWrapper] = deriveDecoder[CommentResponseWrapper]

  def apply(comment: Comment, author: Profile): CommentResponseWrapper =
    CommentResponseWrapper(CommentResponse(comment.id, comment.createdAt, comment.updatedAt, comment.body, author))
}

object CommentListResponseOutWrapper {
  implicit val CommentListResponseOutWrapperEncoder: Encoder[CommentListResponseOutWrapper] = deriveEncoder[CommentListResponseOutWrapper]
  implicit val CommentListResponseOutWrapperDecoder: Decoder[CommentListResponseOutWrapper] = deriveDecoder[CommentListResponseOutWrapper]
}
