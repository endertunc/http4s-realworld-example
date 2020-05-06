package com.real.world.http4s.model.article

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

import com.real.world.http4s.model.NewTypeImplicits._
import com.real.world.http4s.model._

final case class UpdateArticleInput(title: Option[String], description: Option[String], body: Option[String])
final case class UpdateArticleRequest(title: Option[Title], description: Option[Description], body: Option[ArticleBody])
final case class UpdateArticleInputWrapper(article: UpdateArticleInput)

object UpdateArticleInput {
  implicit val ArticleUpdateRequestInDecoder: Decoder[UpdateArticleInput] = deriveDecoder[UpdateArticleInput]
}

object UpdateArticleInputWrapper {
  implicit val ArticleUpdateRequestInWrapperDecoder: Decoder[UpdateArticleInputWrapper] = deriveDecoder[UpdateArticleInputWrapper]
}
