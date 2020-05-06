package com.real.world.http4s.model.comment

import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.{ Decoder, Encoder }

import com.real.world.http4s.model.NewTypeImplicits._
import com.real.world.http4s.model._

final case class CreateCommentInput(body: String)
final case class CreateCommentRequest(body: CommentBody)
final case class CreateCommentInputWrapper(comment: CreateCommentInput)

object CreateCommentInput {
  implicit val CreateCommentInputEncoder: Encoder[CreateCommentInput] = deriveEncoder[CreateCommentInput]
  implicit val CreateCommentInputDecoder: Decoder[CreateCommentInput] = deriveDecoder[CreateCommentInput]
}

object CreateCommentInputWrapper {
  implicit val CreateCommentWrapperEncoder: Encoder[CreateCommentInputWrapper] = deriveEncoder[CreateCommentInputWrapper]
  implicit val CreateCommentWrapperDecoder: Decoder[CreateCommentInputWrapper] = deriveDecoder[CreateCommentInputWrapper]
}
