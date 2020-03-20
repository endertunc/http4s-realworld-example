package com.real.world.http4s.model.article

import java.time.Instant

import com.real.world.http4s.json.{ CirceSchemaValidatorWrapper, ValueClassSchemaValidators }
import com.real.world.http4s.model.article.Article.{ ArticleBody, Description, Title }
import com.real.world.http4s.json.{ CirceSchemaValidatorWrapper, ValueClassSchemaValidators }
import com.real.world.http4s.model.article.Article.{ ArticleBody, Description, Title }

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import json.Schema

final case class UpdateArticle(title: Option[Title], description: Option[Description], body: Option[ArticleBody])
final case class UpdateArticleWrapper(article: UpdateArticle)

object UpdateArticle {

  implicit val ArticleUpdateRequestInDecoder: Decoder[UpdateArticle] = deriveDecoder[UpdateArticle]

  implicit class articleUpdateToArticle(articleUpdateIn: UpdateArticle) {
    def merge(article: Article): Article = article.copy(
      slug        = articleUpdateIn.title.map(_.toSlug).getOrElse(article.slug),
      title       = articleUpdateIn.title.getOrElse(article.title),
      description = articleUpdateIn.description.getOrElse(article.description),
      body        = articleUpdateIn.body.getOrElse(article.body),
      updatedAt   = Instant.now()
    )
  }
}

object UpdateArticleWrapper extends ValueClassSchemaValidators {
  implicit val ArticleUpdateRequestInWrapperDecoder: Decoder[UpdateArticleWrapper] = deriveDecoder[UpdateArticleWrapper]
  implicit val UpdateArticleRequestInWrapperSchema: Schema[UpdateArticleWrapper]   = json.Json.schema[UpdateArticleWrapper]
  implicit val UpdateArticleRequestInWrapperValidatorImpl: CirceSchemaValidatorWrapper[UpdateArticleWrapper] =
    new CirceSchemaValidatorWrapper[UpdateArticleWrapper]("UpdateArticleWrapper")

}
