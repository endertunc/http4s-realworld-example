package com.real.world.http4s.model

import scala.util.control.NoStackTrace

import cats.data.ValidatedNec

trait ValidationErrors extends NoStackTrace { def message: String }

object ValidationErrors {

  type FieldValidationResult[A] = ValidatedNec[InvalidField, A]

  case class InvalidField(field: String, message: String) extends ValidationErrors

}
