package com.real.world.http4s.repository.algebra

import cats.data.NonEmptyList
import com.colisweb.tracing.core.TracingContext
import com.real.world.http4s.model._
import com.real.world.http4s.model.article.Article
import com.real.world.http4s.model.tag.Tag.TagName
import com.real.world.http4s.model.Pagination
import com.real.world.http4s.model.article.FavoritedRecord

trait ArticleRepositoryAlgebra[F[_]] {
  // read
  def findBySlug(slug: Slug)(implicit tracingContext: TracingContext[F]): F[Option[Article]]
  def findFavoritedRecordsByArticleId(articleId: ArticleId)(implicit tracingContext: TracingContext[F]): F[List[FavoritedRecord]]
  def listArticlesByFavoritedUsers(userId: UserId, pagination: Pagination)(implicit tracingContext: TracingContext[F]): F[List[Article]]
  def listArticles(
      maybeAuthorUsername: Option[Username],
      maybeFavoritedAuthorUsername: Option[Username],
      maybeTagName: Option[TagName],
      pagination: Pagination
  )(implicit tracingContext: TracingContext[F]): F[List[Article]]

  // write
  def createArticle(article: Article)(implicit tracingContext: TracingContext[F]): F[Article]
  def updateArticle(article: Article)(implicit tracingContext: TracingContext[F]): F[Option[Article]]

  //delete
  def deleteBySlugAndUserId(slug: Slug, userId: UserId)(implicit tracingContext: TracingContext[F]): F[Unit]

  // utils
  def findArticlesFavoriteCount(articleIds: NonEmptyList[ArticleId])(implicit tracingContext: TracingContext[F]): F[Map[ArticleId, FavoritesCount]]
  def findFavoritedsByUserId(articleIds: NonEmptyList[ArticleId], userId: UserId)(implicit tracingContext: TracingContext[F]): F[Set[ArticleId]]

  // ops
  def favorite(articleId: ArticleId, userId: UserId)(implicit tracingContext: TracingContext[F]): F[FavoritedRecord]
  def unfavorite(articleId: ArticleId, userId: UserId)(implicit tracingContext: TracingContext[F]): F[Unit]
  def isFavoritedByUser(articleId: ArticleId, userId: UserId)(implicit tracingContext: TracingContext[F]): F[Boolean]

}
