package com.real.world.http4s.model.article

import java.time.Instant

import com.real.world.http4s.json.{ CirceSchemaValidatorWrapper, ValueClassSchemaValidators }
import com.real.world.http4s.model.article.Article.{ ArticleBody, ArticleId, Description, Title }
import com.real.world.http4s.model.tag.TagIn
import com.real.world.http4s.model.user.User.UserId
import com.real.world.http4s.json.{ CirceSchemaValidatorWrapper, ValueClassSchemaValidators }
import com.real.world.http4s.model.article.Article.{ ArticleBody, Description, Title }
import com.real.world.http4s.model.tag.TagIn

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import json.Schema

final case class CreateArticle(title: Title, description: Description, body: ArticleBody, tagList: Option[List[TagIn]])
final case class CreateArticleWrapper(article: CreateArticle)

object CreateArticle {
  implicit val CreateArticleDecoder: Decoder[CreateArticle] = deriveDecoder[CreateArticle]

  implicit class toArticle(articleRequestIn: CreateArticle) {
    def toArticle(authorId: UserId): Article =
      Article(
        ArticleId(-1),
        articleRequestIn.title,
        articleRequestIn.description,
        articleRequestIn.body,
        Instant.now,
        Instant.now,
        authorId
      )
  }
}

object CreateArticleWrapper extends ValueClassSchemaValidators {
  implicit val CreateArticleWrapperDecoder: Decoder[CreateArticleWrapper]    = deriveDecoder[CreateArticleWrapper]
  implicit val CreateArticleWrapperCirceSchema: Schema[CreateArticleWrapper] = json.Json.schema[CreateArticleWrapper]
  implicit val CreateArticleWrapperValidatorImpl: CirceSchemaValidatorWrapper[CreateArticleWrapper] =
    new CirceSchemaValidatorWrapper[CreateArticleWrapper]("CreateArticleWrapper")
}
