package com.real.world.http4s.repository.algebra

import cats.data.NonEmptyList
import com.colisweb.tracing.core.TracingContext
import com.real.world.http4s.model._
import com.real.world.http4s.model.tag.Tag
import com.real.world.http4s.model.tag.Tag.TagName

trait TagRepositoryAlgebra[F[_]] {

  def findAll()(implicit tracingContext: TracingContext[F]): F[List[Tag]]
  def createTags(tags: List[TagName])(implicit tracingContext: TracingContext[F]): F[List[Tag]]
  def findTagsByArticleId(articleId: ArticleId)(implicit tracingContext: TracingContext[F]): F[List[Tag]]
  def insertArticleTagsAssociation(articleId: ArticleId, tags: List[Tag])(implicit tracingContext: TracingContext[F]): F[Unit]
  def findTagsByArticleIds(articleIds: NonEmptyList[ArticleId])(implicit tracingContext: TracingContext[F]): F[Map[ArticleId, List[Tag]]]

}
