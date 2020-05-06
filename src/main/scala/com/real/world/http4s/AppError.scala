package com.real.world.http4s

import scala.util.control.NoStackTrace

import org.http4s.circe._
import org.http4s.{ Response, Status }

import cats.Applicative
import cats.data.{ NonEmptyChain, NonEmptyList }
import cats.implicits._

import com.real.world.http4s.model.FieldError.FieldValidationErrorsToFieldError
import com.real.world.http4s.model.ValidationErrors.InvalidField
import com.real.world.http4s.model.{ Error, ErrorWrapperOut, FieldError }

sealed trait AppError extends NoStackTrace {
  def message: String
  def status: Status
  def toHttpResponse[F[_]]()(implicit F: Applicative[F]): F[Response[F]] =
    Response(status)
      .withEntity(ErrorWrapperOut(Error(NonEmptyList.one(message))))(jsonEncoderOf[F, ErrorWrapperOut])
      .pure[F]

  override def getMessage: String = message
}

trait ConflictErrors extends AppError {
  override def status: Status = Status.Conflict
}

trait BadRequestErrors extends AppError {
  override def status: Status = Status.BadRequest
}

trait NotFoundErrors extends AppError {
  override def status: Status = Status.NotFound
}

trait UnexpectedExceptions extends AppError {
  override def status: Status = Status.InternalServerError
}

object AppError {

  final case class UserNotFound(message: String) extends NotFoundErrors
  final case class FolloweeNotFound(message: String) extends NotFoundErrors
  final case class ArticleNotFound(message: String) extends NotFoundErrors
  final case class RecordNotFound(message: String) extends NotFoundErrors

  final case class PasswordHashFailed(message: String) extends BadRequestErrors
  final case class JwtUserIdMalformed(message: String) extends BadRequestErrors
  final case class InvalidAuthorizationHeader(message: String) extends BadRequestErrors
  final case class MissingAuthorizationHeader(message: String) extends BadRequestErrors

  final case class UserAlreadyExist(message: String) extends ConflictErrors

  final case class InvalidFieldError(errors: NonEmptyChain[InvalidField]) extends AppError {
    def message: String = errors.toNonEmptyList.map(e => e.field -> e.message).toList.toMap.toString
    def status: Status  = Status.BadRequest
    override def toHttpResponse[F[_]]()(implicit F: Applicative[F]): F[Response[F]] =
      Response(status)
        .withEntity(errors.toFieldError)(jsonEncoderOf[F, FieldError])
        .pure[F]
  }

}
