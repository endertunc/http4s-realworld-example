package com.real.world.http4s.generators

import java.time.Instant

import scala.util.Random

import cats.effect.IO

import com.real.world.http4s.model.article.{ Article, CreateArticle, CreateArticleWrapper, UpdateArticle, UpdateArticleWrapper }
import com.real.world.http4s.model.{ article, Slug }
import com.real.world.http4s.authentication.PasswordHasher

import org.scalacheck.Gen

object ArticleGenerator extends ValueClassGens {

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

  private def createArticleRequestInGen: Gen[CreateArticle] =
    for {
      title       <- titleGen
      description <- descriptionGen
      articleBody <- articleBodyGen
      tags        <- Gen.some(Gen.listOfN(Random.nextInt(10) + 1, tagInGen))
    } yield article.CreateArticle(
      title       = title,
      description = description,
      body        = articleBody,
      tagList     = tags
    )

  private def updateArticleRequestInGen: Gen[UpdateArticle] =
    for {
      title       <- titleGen
      description <- descriptionGen
      articleBody <- articleBodyGen
    } yield article.UpdateArticle(
      title       = Some(title),
      description = Some(description),
      body        = Some(articleBody)
    )

  def generateArticle()(implicit tsecPasswordHasher: PasswordHasher[IO]): Article = articleGen.sample.get
  def generateCreateArticleWrapper: CreateArticleWrapper                          = CreateArticleWrapper(createArticleRequestInGen.sample.get)
  def generateUpdateArticleWrapper: UpdateArticleWrapper                          = UpdateArticleWrapper(updateArticleRequestInGen.sample.get)

}
