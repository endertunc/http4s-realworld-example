package com.real.world.http4s.model.article

import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.semiauto.deriveEncoder
import io.circe.{ Decoder, Encoder }

import com.real.world.http4s.model.NewTypeImplicits._
import com.real.world.http4s.model._
import com.real.world.http4s.model.tag.Tag.TagName

final case class CreateArticleInput(title: String, description: String, body: String, tagList: Option[List[String]])
final case class CreateArticleRequest(title: Title, description: Description, body: ArticleBody, tagList: Option[List[TagName]])
final case class CreateArticleInputWrapper(article: CreateArticleInput)

object CreateArticleInput {
  implicit val CreateArticleEncoder: Encoder[CreateArticleInput] = deriveEncoder[CreateArticleInput]
  implicit val CreateArticleDecoder: Decoder[CreateArticleInput] = deriveDecoder[CreateArticleInput]
}

object CreateArticleInputWrapper {
  implicit val CreateArticleWrapperEncoder: Encoder[CreateArticleInputWrapper] = deriveEncoder[CreateArticleInputWrapper]
  implicit val CreateArticleWrapperDecoder: Decoder[CreateArticleInputWrapper] = deriveDecoder[CreateArticleInputWrapper]
}
