package com.real.world.http4s.service

import cats.data.NonEmptyList
import cats.effect.Async
import com.colisweb.tracing.core.TracingContext
import com.real.world.http4s.model._
import com.real.world.http4s.model.tag.Tag
import com.real.world.http4s.model.tag.Tag.TagName
import com.real.world.http4s.repository.algebra.TagRepositoryAlgebra
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger

class TagService[F[_]: Async]()(implicit L: SelfAwareStructuredLogger[F], tagRepositoryAlgebra: TagRepositoryAlgebra[F]) {

  def findAll()(implicit tracingContext: TracingContext[F]): F[List[Tag]] = tagRepositoryAlgebra.findAll

  def findTagsByArticleId(articleId: ArticleId)(implicit tracingContext: TracingContext[F]): F[List[Tag]] =
    tagRepositoryAlgebra.findTagsByArticleId(articleId)

  def createTags(tags: List[TagName])(implicit tracingContext: TracingContext[F]): F[List[Tag]] = tagRepositoryAlgebra.createTags(tags)

  def createArticleTagAssociation(articleId: ArticleId, tags: List[Tag])(implicit tracingContext: TracingContext[F]): F[Unit] =
    tagRepositoryAlgebra.insertArticleTagsAssociation(articleId, tags)

  def findTagsByArticleIds(articleIds: NonEmptyList[ArticleId])(implicit tracingContext: TracingContext[F]): F[Map[ArticleId, List[Tag]]] =
    tagRepositoryAlgebra.findTagsByArticleIds(articleIds)

}

object TagService {

  def apply[F[_]: Async: SelfAwareStructuredLogger]()(implicit tagRepositoryAlgebra: TagRepositoryAlgebra[F]): TagService[F] = new TagService[F]()
}
