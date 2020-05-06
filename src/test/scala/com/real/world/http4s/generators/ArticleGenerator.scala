package com.real.world.http4s.generators

import java.time.Instant

import scala.util.Random

import cats.effect.IO

import com.real.world.http4s.authentication.PasswordHasher
import com.real.world.http4s.model.article.{
  Article,
  CreateArticleInput,
  CreateArticleInputWrapper,
  CreateArticleRequest,
  UpdateArticleInput,
  UpdateArticleInputWrapper,
  UpdateArticleRequest
}
import com.real.world.http4s.model.{ article, Slug }

import org.scalacheck.Gen

object ArticleGenerator extends GeneratorsBase {

  private def articleGen()(implicit tsecPasswordHasher: PasswordHasher[IO]): Gen[Article] =
    for {
      articleId   <- articleIdGen
      title       <- titleGen
      slug        <- Gen.const[Slug](title.toSlug)
      description <- descriptionGen
      articleBody <- articleBodyGen
      authorId    <- Gen.const(UserGenerator.generateUser.id)
    } yield Article(
      id          = articleId,
      slug        = slug,
      title       = title,
      description = description,
      body        = articleBody,
      createdAt   = Instant.now,
      updatedAt   = Instant.now,
      authorId    = authorId
    )

  private def createArticleInputGen: Gen[CreateArticleInput] =
    for {
      title       <- titleGen
      description <- descriptionGen
      articleBody <- articleBodyGen
      tags        <- Gen.some(Gen.listOfN(Random.nextInt(10) + 1, tagNameGen)) // scalastyle:off magic.number
    } yield article.CreateArticleInput(
      title       = title.value.value,
      description = description.value.value,
      body        = articleBody.value.value,
      tagList     = tags.map(_.map(_.value.value))
    )

  private def createArticleRequestGen: Gen[CreateArticleRequest] =
    for {
      title       <- titleGen
      description <- descriptionGen
      articleBody <- articleBodyGen
      tags        <- Gen.some(Gen.listOfN(Random.nextInt(10) + 1, tagNameGen)) // scalastyle:off magic.number
    } yield CreateArticleRequest(
      title       = title,
      description = description,
      body        = articleBody,
      tagList     = tags
    )

  private def updateArticleRequestInputGen: Gen[UpdateArticleInput] =
    for {
      title       <- titleGen
      description <- descriptionGen
      articleBody <- articleBodyGen
    } yield UpdateArticleInput(
      title       = Some(title.value.value),
      description = Some(description.value.value),
      body        = Some(articleBody.value.value)
    )

  private def updateArticleRequestRequestGen: Gen[UpdateArticleRequest] =
    for {
      title       <- titleGen
      description <- descriptionGen
      articleBody <- articleBodyGen
    } yield UpdateArticleRequest(
      title       = Some(title),
      description = Some(description),
      body        = Some(articleBody)
    )

  def generateArticle()(implicit tsecPasswordHasher: PasswordHasher[IO]): Article = articleGen.sample.get
  def generateCreateArticleRequest: CreateArticleRequest                          = createArticleRequestGen.sample.get
  def generateCreateArticleWrapper: CreateArticleInputWrapper                     = CreateArticleInputWrapper(createArticleInputGen.sample.get)
  def generateUpdateArticleRequest: UpdateArticleRequest                          = updateArticleRequestRequestGen.sample.get
  def generateUpdateArticleInputWrapper: UpdateArticleInputWrapper                = UpdateArticleInputWrapper(updateArticleRequestInputGen.sample.get)

}
