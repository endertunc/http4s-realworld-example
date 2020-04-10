package com.real.world.http4s.model.article

import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.semiauto.deriveEncoder
import io.circe.refined._
import io.circe.{ Decoder, Encoder }

import com.real.world.http4s.model.Instances._
import com.real.world.http4s.model._
import com.real.world.http4s.model.tag.TagIn

final case class CreateArticle(title: Title, description: Description, body: ArticleBody, tagList: Option[List[TagIn]])
final case class CreateArticleWrapper(article: CreateArticle)

object CreateArticle {

  implicit val CreateArticleEncoder: Encoder[CreateArticle] = deriveEncoder[CreateArticle]
  implicit val CreateArticleDecoder: Decoder[CreateArticle] = deriveDecoder[CreateArticle]

}

object CreateArticleWrapper {
  implicit val CreateArticleWrapperEncoder: Encoder[CreateArticleWrapper] = deriveEncoder[CreateArticleWrapper]
  implicit val CreateArticleWrapperDecoder: Decoder[CreateArticleWrapper] = deriveDecoder[CreateArticleWrapper]

}
