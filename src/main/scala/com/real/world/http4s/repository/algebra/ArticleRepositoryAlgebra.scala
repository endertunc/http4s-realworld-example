package com.real.world.http4s.repository.algebra

import com.real.world.http4s.model.article.Article.{ ArticleId, FavoritesCount, Slug }
import com.real.world.http4s.model.tag.Tag.TagName
import com.real.world.http4s.model.user.User.{ UserId, Username }
import com.real.world.http4s.model.{ FavoritedRecord, Pagination }
import com.real.world.http4s.model.{ FavoritedRecord, Pagination }
import com.real.world.http4s.model.article.Article

import cats.data.NonEmptyList

trait ArticleRepositoryAlgebra[F[_]] {
  // read
  def findBySlug(slug: Slug): F[Option[Article]]
  def findFavoritedRecordsByArticleId(articleId: ArticleId): F[List[FavoritedRecord]]
  def listArticles(
      maybeAuthorUsername: Option[Username],
      maybeFavoritedAuthorUsername: Option[Username],
      maybeTagName: Option[TagName],
      pagination: Pagination
  ): F[List[Article]]
  def listArticlesByFavoritedUsers(userId: UserId, pagination: Pagination): F[List[Article]]

  // write
  def createArticle(article: Article): F[Article]
  def updateArticle(article: Article): F[Option[Article]]

  //delete
  def deleteBySlugAndUserId(slug: Slug, userId: UserId): F[Unit]

  // utils
  def findArticlesFavoriteCount(articleIds: NonEmptyList[ArticleId]): F[Map[ArticleId, FavoritesCount]]
  def findFavoritedsByUserId(articleIds: NonEmptyList[ArticleId], userId: UserId): F[Set[ArticleId]]

  // ops
  def favorite(articleId: ArticleId, userId: UserId): F[FavoritedRecord]
  def unfavorite(articleId: ArticleId, userId: UserId): F[Unit]
  def isFavoritedByUser(articleId: ArticleId, userId: UserId): F[Boolean]

}
