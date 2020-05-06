package com.real.world.http4s.generators

import com.real.world.http4s.model.user
import com.real.world.http4s.model.user.UpdateUserWrapper
import com.real.world.http4s.model.user.{ UpdateUser, UpdateUserWrapper }

import org.scalacheck.Gen

object UpdateUserGenerator extends GeneratorsBase {

  private def updateUserGen: Gen[UpdateUser] =
    for {
      email    <- emailGen
      username <- usernameGen
      password <- passwordGen
      bio      <- bioGen
      image    <- imageGen
    } yield user.UpdateUser(
      email    = Some(email),
      username = Some(username),
      password = Some(password),
      bio      = bio,
      image    = image
    )

  def generateUpdateUserWrapper: UpdateUserWrapper = UpdateUserWrapper(updateUserGen.sample.get)

}
