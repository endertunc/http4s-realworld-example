package com.real.world.http4s.model.comment

import Comment.CommentBody
import com.real.world.http4s.json.ValueClassSchemaValidators
import com.real.world.http4s.json.{ CirceSchemaValidatorWrapper, ValueClassSchemaValidators }

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import json.Schema

final case class CreateComment(body: CommentBody)
final case class CreateCommentWrapper(comment: CreateComment)

object CreateComment {
  implicit val CreateCommentCodec: Codec[CreateComment] = deriveCodec[CreateComment]
}

object CreateCommentWrapper extends ValueClassSchemaValidators {
  implicit val CreateCommentWrapperDecoder: Codec[CreateCommentWrapper]      = deriveCodec[CreateCommentWrapper]
  implicit val CreateCommentWrapperCirceSchema: Schema[CreateCommentWrapper] = json.Json.schema[CreateCommentWrapper]
  implicit val CreateCommentWrapperValidatorImpl: CirceSchemaValidatorWrapper[CreateCommentWrapper] =
    new CirceSchemaValidatorWrapper[CreateCommentWrapper]("CreateCommentWrapper")
}
