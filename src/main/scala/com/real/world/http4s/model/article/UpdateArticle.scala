package com.real.world.http4s.model.article

import java.time.Instant

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.refined._

import com.real.world.http4s.model.Instances._
import com.real.world.http4s.model._

final case class UpdateArticle(title: Option[Title], description: Option[Description], body: Option[ArticleBody])
final case class UpdateArticleWrapper(article: UpdateArticle)

object UpdateArticle {

  implicit val ArticleUpdateRequestInDecoder: Decoder[UpdateArticle] = deriveDecoder[UpdateArticle]

  implicit class articleUpdateToArticle(articleUpdateIn: UpdateArticle) {
    def merge(article: Article): Article = article.copy(
      title       = articleUpdateIn.title.getOrElse(article.title),
      slug        = articleUpdateIn.title.map(_.toSlug).getOrElse(article.slug),
      description = articleUpdateIn.description.getOrElse(article.description),
      body        = articleUpdateIn.body.getOrElse(article.body),
      updatedAt   = Instant.now()
    )
  }
}

object UpdateArticleWrapper {
  implicit val ArticleUpdateRequestInWrapperDecoder: Decoder[UpdateArticleWrapper] = deriveDecoder[UpdateArticleWrapper]

}
