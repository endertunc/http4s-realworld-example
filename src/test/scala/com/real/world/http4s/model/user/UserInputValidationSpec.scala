package com.real.world.http4s.model.user

import org.http4s.circe.CirceEntityDecoder

import cats.data.NonEmptyChain
import cats.scalatest.ValidatedMatchers
import cats.scalatest.ValidatedValues

import com.real.world.http4s.generators.GeneratorsBase
import com.real.world.http4s.model
import com.real.world.http4s.model.ValidationErrors.InvalidField

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class UserInputValidationSpec
    extends AsyncFlatSpec
    with UserInputValidator
    with Matchers
    with ValidatedMatchers
    with ValidatedValues
    with GeneratorsBase
    with CirceEntityDecoder {

  val emptyString             = ""
  val nonEmptyString          = "NonEmptyString"
  val validEmail: model.Email = emailGen.sample.get

  "UserInputValidator" should "validate email" in {
    validateEmail(emptyString) should beInvalid(NonEmptyChain.one(InvalidField("email", "must be a valid email")))
    validateEmail(validEmail.value.value) should be(valid)
  }

  it should "validate password" in {
    validatePassword(emptyString) should beInvalid(NonEmptyChain.one(InvalidField("password", "Todo")))
    validatePassword(nonEmptyString) should be(valid)
  }

  it should "validate username" in {
    validateUsername(emptyString) should beInvalid(NonEmptyChain.one(InvalidField("username", "Todo")))
    validateUsername(nonEmptyString) should be(valid)
  }

}
