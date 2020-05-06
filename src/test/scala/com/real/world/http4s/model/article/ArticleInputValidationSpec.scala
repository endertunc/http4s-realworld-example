package com.real.world.http4s.model.article

import org.http4s.circe.CirceEntityDecoder

import cats.data.NonEmptyChain
import cats.scalatest.ValidatedMatchers
import cats.scalatest.ValidatedValues

import com.real.world.http4s.generators.GeneratorsBase
import com.real.world.http4s.model.ValidationErrors.InvalidField

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class ArticleInputValidationSpec
    extends AsyncFlatSpec
    with ArticleInputValidator
    with Matchers
    with ValidatedMatchers
    with ValidatedValues
    with GeneratorsBase
    with CirceEntityDecoder {

  val emptyString    = ""
  val nonEmptyString = "NonEmptyString"

  "ArticleInputValidator" should "validate title" in {
    validateTitle(emptyString) should beInvalid(NonEmptyChain.one(InvalidField("title", "Todo")))
    validateTitle(nonEmptyString) should be(valid)
  }

  it should "validate description" in {
    validateDescription(emptyString) should beInvalid(NonEmptyChain.one(InvalidField("description", "Todo")))
    validateDescription(nonEmptyString) should be(valid)
  }

  it should "validate article body" in {
    validateArticleBody(emptyString) should beInvalid(NonEmptyChain.one(InvalidField("body", "Todo")))
    validateArticleBody(nonEmptyString) should be(valid)
  }

  it should "validate tag name" in {
    validateArticleTags(List(emptyString)) should beInvalid(NonEmptyChain.one(InvalidField("tag", "Todo")))
    validateArticleTags(List(nonEmptyString)) should be(valid)
  }

}
