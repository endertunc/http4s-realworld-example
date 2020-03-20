package com.real.world.http4s.generators

import com.real.world.http4s.model.user
import com.real.world.http4s.model.user.RegisterUser
import org.scalacheck.Gen

object UserRegisterGenerator extends ValueClassGens {

  private val userRegisterGen: Gen[RegisterUser] =
    for {
      email    <- emailGen
      password <- passwordGen
      username <- usernameGen
    } yield user.RegisterUser(
      email    = email,
      password = password,
      username = username
    )

  def generateUserRegister: RegisterUser = userRegisterGen.sample.get
}
