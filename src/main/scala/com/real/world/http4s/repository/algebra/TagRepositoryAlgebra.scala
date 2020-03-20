package com.real.world.http4s.repository.algebra

import com.real.world.http4s.model.article.Article.ArticleId
import com.real.world.http4s.model.tag.Tag
import com.real.world.http4s.model.tag.{ Tag, TagIn }

import cats.data.NonEmptyList

trait TagRepositoryAlgebra[F[_]] {

  def findAll: F[List[Tag]]
  def createTags(tags: List[TagIn]): F[List[Tag]]
  def findTagsByArticleId(articleId: ArticleId): F[List[Tag]]
  def insertArticleTagsAssociation(articleId: ArticleId, tags: List[Tag]): F[Unit]
  def findTagsByArticleIds(articleIds: NonEmptyList[ArticleId]): F[Map[ArticleId, List[Tag]]]

}
