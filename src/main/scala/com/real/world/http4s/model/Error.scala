package com.real.world.http4s.model

import cats.data.NonEmptyList

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import io.circe.schema.ValidationError

case class Error(errors: NonEmptyList[String])
case class ErrorWrapperOut(error: Error)

object Error {
  implicit val ErrorEncoder: Encoder[Error] = deriveEncoder[Error]
}

object ErrorWrapperOut {
  implicit val ErrorWrapperOutEncoder: Encoder[ErrorWrapperOut] = deriveEncoder[ErrorWrapperOut]

  def fromString(error: String): ErrorWrapperOut = ErrorWrapperOut(Error(NonEmptyList.of(error)))

  implicit class JsonSchemaErrorToErrorWrapper(errors: NonEmptyList[ValidationError]) {
    def toErrorWrapper: ErrorWrapperOut = ErrorWrapperOut(Error(errors.map(_.getMessage)))
  }
}
