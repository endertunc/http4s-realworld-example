package com.real.world.http4s.model.comment

import java.time.Instant

import Comment.{ CommentBody, CommentId }
import com.real.world.http4s.model.profile.Profile
import com.real.world.http4s.model.profile.Profile

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class CommentResponse(id: CommentId, createdAt: Instant, updatedAt: Instant, body: CommentBody, author: Profile)
final case class CommentResponseWrapper(comment: CommentResponse)
final case class CommentListResponseOutWrapper(comments: List[CommentResponse])

object CommentResponse {
  implicit val crEncoder: Codec[CommentResponse] = deriveCodec[CommentResponse]

  def apply(comment: Comment, author: Profile): CommentResponse =
    CommentResponse(comment.id, comment.createdAt, comment.updatedAt, comment.body, author)
}

object CommentResponseWrapper {
  implicit val CommentResponseWrapperCodec: Codec[CommentResponseWrapper] = deriveCodec[CommentResponseWrapper]

  def apply(comment: Comment, author: Profile): CommentResponseWrapper =
    CommentResponseWrapper(CommentResponse(comment.id, comment.createdAt, comment.updatedAt, comment.body, author))
}

object CommentListResponseOutWrapper {
  implicit val CommentListResponseOutWrapperEncoder: Codec[CommentListResponseOutWrapper] = deriveCodec[CommentListResponseOutWrapper]
}
