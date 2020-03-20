package com.real.world.http4s.generators

import com.real.world.http4s.model.user.{ RegisterUser, UserLogin, UserLoginWrapper }
import com.real.world.http4s.model.user
import org.scalacheck.Gen

object UserLoginGenerator extends ValueClassGens {

  private val userLoginRequestInGen: Gen[UserLogin] =
    for {
      email    <- emailGen
      password <- passwordGen
    } yield user.UserLogin(
      email    = email,
      password = password
    )

  def generateUserLogin: UserLogin               = userLoginRequestInGen.sample.get
  def generateUserLoginWrapper: UserLoginWrapper = UserLoginWrapper(userLoginRequestInGen.sample.get)

  def fromUserRegister(userRegisterRequestIn: RegisterUser): UserLogin =
    UserLogin(userRegisterRequestIn.email, userRegisterRequestIn.password)

//  def fromUserRegister(userRegisterRequestIn: RegisterUser): UserLoginWrapper =
//    UserLoginWrapper(UserLogin(userRegisterRequestIn.email, userRegisterRequestIn.password))
}
