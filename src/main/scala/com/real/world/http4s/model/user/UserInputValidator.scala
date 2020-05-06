package com.real.world.http4s.model.user

import cats.effect.Sync
import cats.implicits._

import com.real.world.http4s.AppError.InvalidFieldError
import com.real.world.http4s.model.ValidationErrors.{ FieldValidationResult, InvalidField }
import com.real.world.http4s.model.{ Email, PlainTextPassword, Username }

import io.chrisdavenport.log4cats.Logger

trait UserInputValidator {

  protected def validateEmail(email: String): FieldValidationResult[Email] =
    Email.from(email).fold(_ => InvalidField("email", "must be a valid email").invalidNec, _.validNec)

  protected def validateUsername(username: String): FieldValidationResult[Username] =
    Username.from(username).fold(_ => InvalidField("username", "Todo").invalidNec, _.validNec)

  // ToDo double check err.getMessage
  protected def validatePassword(password: String): FieldValidationResult[PlainTextPassword] =
    PlainTextPassword.from(password).fold(err => InvalidField("password", "Todo").invalidNec, _.validNec)

  def validateRegisterUserInput[F[_]: Sync: Logger](registerUser: RegisterUserInput): F[RegisterUserRequest] = {
    import registerUser._
    (
      validateEmail(email),
      validateUsername(username),
      validatePassword(password)
    ).tupled.fold(
      InvalidFieldError(_).raiseError[F, RegisterUserRequest], {
        case (email, username, password) => RegisterUserRequest(email, username, password).pure[F]
      }
    )
  }

  def validateUserLoginInput[F[_]: Sync: Logger](userLogin: UserLoginInput): F[UserLoginRequest] = {
    import userLogin._
    (
      validateEmail(email),
      validatePassword(password)
    ).tupled.fold(
      InvalidFieldError(_).raiseError[F, UserLoginRequest], {
        case (email, password) => UserLoginRequest(email, password).pure[F]
      }
    )
  }

}
