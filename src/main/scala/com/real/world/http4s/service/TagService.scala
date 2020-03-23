package com.real.world.http4s.service

import cats.data.NonEmptyList
import cats.effect.Async

import com.real.world.http4s.model._
import com.real.world.http4s.model.tag.{ Tag, TagIn }
import com.real.world.http4s.repository.algebra.TagRepositoryAlgebra

import io.chrisdavenport.log4cats.Logger

class TagService[F[_]: Async: Logger]()(implicit tagRepositoryAlgebra: TagRepositoryAlgebra[F]) {

  def findAll: F[List[Tag]] = tagRepositoryAlgebra.findAll

  def findTagsByArticleId(articleId: ArticleId): F[List[Tag]] = tagRepositoryAlgebra.findTagsByArticleId(articleId)

  def createTags(tags: List[TagIn]): F[List[Tag]] = tagRepositoryAlgebra.createTags(tags)

  def createArticleTagAssociation(articleId: ArticleId, tags: List[Tag]): F[Unit] =
    tagRepositoryAlgebra.insertArticleTagsAssociation(articleId, tags)

  def findTagsByArticleIds(articleIds: NonEmptyList[ArticleId]): F[Map[ArticleId, List[Tag]]] =
    tagRepositoryAlgebra.findTagsByArticleIds(articleIds)

}

object TagService {

  def apply[F[_]: Async: Logger]()(implicit tagRepositoryAlgebra: TagRepositoryAlgebra[F]): TagService[F] = new TagService[F]()
}
