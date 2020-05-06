package com.real.world.http4s.generators

import com.real.world.http4s.model.user.{ RegisterUserInput, RegisterUserRequest, UserLoginInput, UserLoginInputWrapper, UserLoginRequest }

import org.scalacheck.Gen

object UserLoginGenerator extends GeneratorsBase {

  private val userLoginRequestInGen: Gen[UserLoginInput] =
    for {
      email    <- emailGen
      password <- passwordGen
    } yield UserLoginInput(
      email    = email.value.value,
      password = password.value.value
    )

  def generateUserLogin: UserLoginInput               = userLoginRequestInGen.sample.get
  def generateUserLoginWrapper: UserLoginInputWrapper = UserLoginInputWrapper(userLoginRequestInGen.sample.get)

  def fromUserRegister(userRegisterRequestIn: RegisterUserInput): UserLoginInput =
    UserLoginInput(userRegisterRequestIn.email, userRegisterRequestIn.password)

  def fromUserRegisterRequest(registerUserRequest: RegisterUserRequest): UserLoginRequest =
    UserLoginRequest(registerUserRequest.email, registerUserRequest.password)

  def fromUserRegisterRequestToUserLoginInput(registerUserRequest: RegisterUserRequest): UserLoginInput =
    UserLoginInput(registerUserRequest.email.value.value, registerUserRequest.password.value.value)

}
