package com.real.world.http4s.model.comment

import cats.effect.Sync
import cats.implicits._

import com.real.world.http4s.AppError.InvalidFieldError
import com.real.world.http4s.model.CommentBody
import com.real.world.http4s.model.ValidationErrors.{ FieldValidationResult, InvalidField }

import io.chrisdavenport.log4cats.Logger

trait CommentInputValidator {

  protected def validateCommentBody(body: String): FieldValidationResult[CommentBody] =
    CommentBody.from(body).fold(err => InvalidField("body", "Todo Reason").invalidNec, _.validNec)

  def validateCreateCommentInput[F[_]: Sync: Logger](createCommentInput: CreateCommentInput): F[CreateCommentRequest] =
    validateCommentBody(createCommentInput.body).fold(
      InvalidFieldError(_).raiseError[F, CreateCommentRequest],
      body => CreateCommentRequest(body).pure[F]
    )

}
