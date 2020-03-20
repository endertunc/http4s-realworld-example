package com.real.world.http4s.model

import com.real.world.http4s.AppError.DomainValidationFailed
import com.real.world.http4s.model.article.Article.{ ArticleBody, Description, Title }
import com.real.world.http4s.model.article.CreateArticle
import com.real.world.http4s.model.user.User.{ Email, PlainTextPassword, Username }
import com.real.world.http4s.model.user.{ RegisterUser, UpdateUser }
import io.chrisdavenport.log4cats.Logger

import cats.data.Validated._
import cats.data.ValidatedNec
import cats.effect.Sync
import cats.implicits._

sealed trait DomainValidation {
  def errorMessage: String
}

final case class BlankString(errorMessage: String) extends DomainValidation
final case class InvalidStringSize(errorMessage: String) extends DomainValidation

case object InvalidEmail extends DomainValidation {
  def errorMessage: String = "String hasn't passed email validation"
}

case object UsernameDoesNotMeetCriteria extends DomainValidation {
  def errorMessage: String = "Username must be at least 3 characters long and can not contain any special characters"
}

case object PasswordDoesNotMeetCriteria extends DomainValidation {
  def errorMessage: String = "Password must be at least 6 characters long and can not contain any special characters"
}

/**
  * All validation are there only for demonstration purposes.
  * Real World API does not enforce anything really and I need to make sure that run-api-test.sh still works :)
  */
sealed trait ValidationRules {

  type ValidationResult[A] = ValidatedNec[DomainValidation, A]

  // scalastyle:off line.size.limit
  // https://github.com/playframework/playframework/blob/ddf3a7ee4285212ec665826ec268ef32b5a76000/core/play/src/main/scala/play/api/data/validation/Validation.scala#L79
  private val emailPattern =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""
  // scalastyle:on line.size.limit

  private val passwordPattern: String = """^[a-zA-Z\d]{6,20}$"""

  private val usernamePattern = """^[a-zA-Z\d]{3,10}$"""

  def validateEmail(email: Email): ValidationResult[Email] =
    if (email.value.matches(emailPattern)) email.validNec else InvalidEmail.invalidNec

  def validateNonEmpty(fieldName: String, value: String): ValidationResult[String] =
    if (value.isBlank) BlankString(s"$fieldName cannot be blank").invalidNec else value.validNec

  def validateUsername(username: Username): ValidationResult[Username] =
    if (username.value.matches(usernamePattern)) username.validNec else UsernameDoesNotMeetCriteria.invalidNec

  def validatePassword(plainTextPassword: PlainTextPassword): ValidationResult[PlainTextPassword] =
    if (plainTextPassword.value.matches(passwordPattern)) plainTextPassword.validNec else PasswordDoesNotMeetCriteria.invalidNec

  def validateTitle(title: Title): ValidationResult[Title] = validateStringLength("title", title.value).map(Title)

  def validateDescription(description: Description): ValidationResult[Description] =
    validateStringLength("description", description.value).map(Description)

  def validateBody(articleBody: ArticleBody): ValidationResult[ArticleBody] =
    validateStringLength("articleBody", articleBody.value, 255, Int.MaxValue).map(ArticleBody) // scalastyle:ignore

  def validateStringLength(fieldName: String, value: String, min: Int = 6, max: Int = 255): ValidationResult[String] = // scalastyle:ignore
    if (value.length >= min && value.length <= max) value.validNec else InvalidStringSize(s"${fieldName} length is out range [$min; $max]").invalidNec

}

trait ArticleValidators extends ValidationRules {

  def validateCreateArticle[F[_]: Sync: Logger](
      createArticle: CreateArticle
  ): F[Unit] =
    (validateTitle(createArticle.title), validateDescription(createArticle.description), validateBody(createArticle.body)).tupled
      .fold(
        { nec =>
          val err = DomainValidationFailed(nec)
          Logger[F].info(err.message) *> err.raiseError[F, Unit]
        },
        _ => ().pure[F]
      )

}

trait UserValidators extends ValidationRules {
  def validateUpdateUser[F[_]: Sync: Logger](updateUser: UpdateUser): F[Unit] =
    (
      updateUser.email.traverse(validateEmail),
      updateUser.password.traverse(validatePassword),
      updateUser.username.traverse(validateUsername)
    ).tupled.fold(
      { nec =>
        val err = DomainValidationFailed(nec)
        Logger[F].info(err.message) *> err.raiseError[F, Unit]
      },
      _ => ().pure[F]
    )

  def validateRegisterUser[F[_]: Sync: Logger](
      registerUser: RegisterUser
  ): F[Unit] =
    (validateEmail(registerUser.email), validatePassword(registerUser.password), validateUsername(registerUser.username)).tupled
      .fold(
        { nec =>
          val err = DomainValidationFailed(nec)
          Logger[F].info(err.message) *> err.raiseError[F, Unit]
        },
        _ => ().pure[F]
      )

}
