package com.real.world.http4s.generators

import com.real.world.http4s.model.user
import com.real.world.http4s.model.user.{ RegisterUserInput, RegisterUserRequest }

import org.scalacheck.Gen

object UserRegisterGenerator extends GeneratorsBase {

  private val userRegisterInputGen: Gen[RegisterUserInput] =
    for {
      email    <- emailGen
      password <- passwordGen
      username <- usernameGen
    } yield user.RegisterUserInput(
      email    = email.value.value,
      password = password.value.value,
      username = username.value.value
    )

  private val userRegisterRequestGen: Gen[RegisterUserRequest] =
    for {
      email    <- emailGen
      password <- passwordGen
      username <- usernameGen
    } yield user.RegisterUserRequest(
      email    = email,
      password = password,
      username = username
    )

  def generateUserRegisterInput: RegisterUserInput     = userRegisterInputGen.sample.get
  def generateUserRegisterRequest: RegisterUserRequest = userRegisterRequestGen.sample.get

}
