package com.real.world.http4s.model.article

import java.time.Instant

import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.semiauto.deriveEncoder
import io.circe.refined._
import io.circe.{ Encoder, Decoder }

import com.real.world.http4s.model.Instances._
import com.real.world.http4s.model.Instances._
import com.real.world.http4s.model._

import eu.timepit.refined.types.numeric.NonNegInt
import io.scalaland.chimney.dsl.TransformerOps

final case class Article(
    id: ArticleId,
    slug: Slug,
    title: Title,
    description: Description,
    body: ArticleBody,
    createdAt: Instant,
    updatedAt: Instant,
    authorId: UserId
)

object Article {

  implicit val ArticleEncoder: Encoder[Article] = deriveEncoder[Article]
  implicit val ArticleDecoder: Decoder[Article] = deriveDecoder[Article]

  def fromCreateArticle(createArticle: CreateArticle, authorId: UserId): Article =
    createArticle
      .into[Article]
      .withFieldConst(_.id, ArticleId(NonNegInt.MinValue))
      .withFieldComputed(_.slug, article => article.title.toSlug)
      .withFieldConst(_.authorId, authorId)
      .withFieldConst(_.updatedAt, Instant.now)
      .withFieldConst(_.createdAt, Instant.now)
      .transform

  def fromUpdateArticle(article: Article, updateArticle: UpdateArticle): Article =
    updateArticle
      .into[Article]
      .withFieldConst(_.id, article.id)
      .withFieldComputed(_.title, _.title.getOrElse(article.title))
      .withFieldComputed(_.slug, _.title.map(_.toSlug).getOrElse(article.slug))
      .withFieldComputed(_.description, _.description.getOrElse(article.description))
      .withFieldComputed(_.body, _.body.getOrElse(article.body))
      .withFieldConst(_.authorId, article.authorId)
      .withFieldConst(_.updatedAt, Instant.now)
      .withFieldConst(_.createdAt, article.createdAt)
      .transform

}
