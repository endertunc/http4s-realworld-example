package com.real.world.http4s.repository

import java.time.Instant

import cats.data.NonEmptyList
import cats.effect.{ Async, Sync }
import cats.free.Free
import cats.implicits._

import doobie.Fragments.whereOrOpt
import doobie.free.connection
import doobie.implicits._
import doobie.implicits.legacy.instant.JavaTimeInstantMeta
import doobie.refined.implicits._
import doobie.{ ConnectionIO, _ }

import com.real.world.http4s.AppError.{ ArticleNotFound, RecordNotFound }
import com.real.world.http4s.model.Instances._
import com.real.world.http4s.model._
import com.real.world.http4s.model.article.Article
import com.real.world.http4s.model.tag.Tag.TagName
import com.real.world.http4s.model.{ FavoritedRecord, Pagination }
import com.real.world.http4s.repository.algebra.ArticleRepositoryAlgebra

import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.chrisdavenport.log4cats.{ Logger, SelfAwareStructuredLogger }

class PostgresArticleRepositoryAlgebra[F[_]: Async: Logger]()(implicit xa: Transactor[F]) extends ArticleRepositoryAlgebra[F] {

  implicit private val log: SelfAwareStructuredLogger[ConnectionIO] = Slf4jLogger.getLogger[ConnectionIO]

  override def findBySlug(slug: Slug): F[Option[Article]] = ArticlesStatement.findBySlug(slug).option.transact(xa)

  override def listArticles(
      maybeAuthorUsername: Option[Username],
      maybeFavoritedAuthorUsername: Option[Username],
      maybeTagName: Option[TagName],
      pagination: Pagination
  ): F[List[Article]] =
    ArticlesStatement
      .listArticles(maybeAuthorUsername, maybeFavoritedAuthorUsername, maybeTagName, pagination)
      .to[List]
      .transact(xa)

  override def listArticlesByFavoritedUsers(userId: UserId, pagination: Pagination): F[List[Article]] =
    ArticlesStatement
      .listArticlesByFavoritedUsers(userId, pagination)
      .to[List]
      .transact(xa)

  /**
    * Just demonstrate how to make transaction with doobie.
    * Otherwise, we should be using soft delete in complex relational delete operations.
    */
  override def deleteBySlugAndUserId(slug: Slug, userId: UserId): F[Unit] = {
    val query: Free[connection.ConnectionOp, Unit] =
      for {
        articleAsOpt <- ArticlesStatement.findBySlug(slug).option
        article      <- Sync[ConnectionIO].fromOption(articleAsOpt, ArticleNotFound(s"Article with slug [$slug] is not found"))
        _ <- Sync[ConnectionIO].ifM((article.authorId == userId).pure[ConnectionIO])(
          article.pure[ConnectionIO],
          Logger[ConnectionIO].warn(s"User [${userId}] attempt to delete unauthorized article; id=[${article.id}] slug=[${article.slug}]") *>
          ArticleNotFound(s"Article with slug [$slug] is not found").raiseError[ConnectionIO, Article]
        )
        _ <- CommentStatement.deleteCommentsByArticleId(article.id)
        _ <- TagsStatement.deleteByArticleId(article.id).run
        _ <- ArticlesStatement.deleteByArticleId(article.id).run
      } yield ()
    query.transact(xa)
  }

  override def createArticle(
      article: Article
  ): F[Article] =
    for {
      query            <- ArticlesStatement.createArticle[F](article)
      persistedArticle <- query.unique.transact(xa)
    } yield persistedArticle

  override def favorite(articleId: ArticleId, userId: UserId): F[FavoritedRecord] =
    ArticlesStatement.favorite(articleId, userId).unique.transact(xa)

  override def unfavorite(articleId: ArticleId, userId: UserId): F[Unit] =
    ArticlesStatement
      .unfavorite(articleId, userId)
      .run
      .transact(xa)
      .flatMap {
        case effectedRows: Int if effectedRows == 0 =>
          RecordNotFound(s"User already unfavorited the article").raiseError[F, Unit] <*
          Logger[F].warn("No record found for the following users...")
        case effectedRows: Int if effectedRows == 1 => Sync[F].delay(())
      }

  override def isFavoritedByUser(articleId: ArticleId, userId: UserId): F[Boolean] =
    ArticlesStatement
      .isFavoritedByUser(articleId, userId)
      .option
      .map(_.isDefined)
      .transact(xa)

  override def findFavoritedRecordsByArticleId(articleId: ArticleId): F[List[FavoritedRecord]] =
    ArticlesStatement
      .findArticleFavorites(articleId)
      .to[List]
      .transact(xa)

  override def updateArticle(article: Article): F[Option[Article]] =
    for {
      query   <- ArticlesStatement.updateArticle[F](article)
      article <- query.option.transact(xa)
    } yield article

  override def findArticlesFavoriteCount(articleIds: NonEmptyList[ArticleId]): F[Map[ArticleId, FavoritesCount]] =
    // Is there better way to having map
    ArticlesStatement.findArticlesFavoriteCount(articleIds).to[List].map(_.toMap).transact(xa)

  override def findFavoritedsByUserId(articleIds: NonEmptyList[ArticleId], userId: UserId): F[Set[ArticleId]] =
    ArticlesStatement.findFavoritedsByUserId(articleIds, userId).to[Set].transact(xa)
}

object PostgresArticleRepositoryAlgebra {
  def apply[F[_]: Async: Logger: Transactor](): ArticleRepositoryAlgebra[F] = new PostgresArticleRepositoryAlgebra()
}

object ArticlesStatement {

  def findBySlug(slug: Slug): doobie.Query0[Article] =
    sql"SELECT id, slug, title, description, body, created_at, updated_at, author_id FROM articles WHERE slug = $slug".query[Article]

  def deleteByArticleId(articleId: ArticleId): doobie.Update0 =
    sql"DELETE FROM articles WHERE id=$articleId".update

  def favorite(articleId: ArticleId, userId: UserId): doobie.Query0[FavoritedRecord] =
    sql"INSERT INTO favorite (user_id, favorited_id) VALUES ($userId, $articleId) RETURNING id, user_id, favorited_id".query[FavoritedRecord]

  def unfavorite(articleId: ArticleId, userId: UserId): doobie.Update0 =
    sql"DELETE FROM favorite WHERE user_id=$userId AND favorited_id=$articleId".update

  def createArticle[F[_]: Async](article: Article): F[doobie.Query0[Article]] =
    Async[F].delay(Instant.now).map { now =>
      sql"""
         INSERT INTO articles (slug, title, description, body, created_at, updated_at, author_id)
         VALUES (${article.slug}, ${article.title}, ${article.description}, ${article.body}, $now, $now, ${article.authorId})
         RETURNING id, slug, title, description, body, created_at, updated_at, author_id""".query[Article]
    }

  def updateArticle[F[_]: Async](article: Article): F[doobie.Query0[Article]] =
    Async[F].delay(Instant.now).map { now =>
      sql"""
         UPDATE articles SET 
         slug = ${article.slug}, title = ${article.title},
         description = ${article.description}, body = ${article.body}, updated_at = $now
         WHERE id = ${article.id}
         RETURNING id, slug, title, description, body, created_at, updated_at, author_id""".query[Article]
    }

  // ToDo this should be findFavorited. Service should map it to boolean
  def isFavoritedByUser(articleId: ArticleId, userId: UserId): doobie.Query0[FavoritedRecord] =
    sql"""SELECT id, user_id, favorited_id FROM favorite WHERE user_id=$userId AND favorited_id=$articleId""".query[FavoritedRecord]

  // ToDo this can be implemented as count function in DB level as well and that would be better
  def findArticleFavorites(articleId: ArticleId): doobie.Query0[FavoritedRecord] =
    sql"SELECT id, user_id, favorited_id FROM favorite WHERE favorited_id=$articleId".query[FavoritedRecord]

  def findFavoritedsByUserId(articleIds: NonEmptyList[ArticleId], userId: UserId): doobie.Query0[ArticleId] =
    (fr"SELECT favorited_id FROM favorite WHERE user_id=$userId AND " ++ Fragments.in(fr"favorited_id", articleIds)).query[ArticleId]

  def findArticlesFavoriteCount(articleIds: NonEmptyList[ArticleId]): doobie.Query0[(ArticleId, FavoritesCount)] =
    (fr"SELECT favorited_id, COUNT(user_id) FROM favorite WHERE"
    ++ Fragments.in(fr"favorited_id", articleIds)
    ++ fr"GROUP BY favorited_id")
      .query[(ArticleId, FavoritesCount)]

  def listArticles(
      maybeAuthorUsername: Option[Username],
      maybeFavoritedAuthorUsername: Option[Username],
      maybeTagName: Option[TagName],
      pagination: Pagination
  ): doobie.Query0[Article] = {
    val byAuthor: Option[Fragment] =
      maybeAuthorUsername.map(username => fr"""articles.author_id IN (SELECT users.id FROM users WHERE username=$username LIMIT 1)""")

    val byFavoritedAuthor: Option[Fragment] =
      maybeFavoritedAuthorUsername.map(username => fr"""
          articles.id IN (
            SELECT favorite.favorited_id FROM favorite
            INNER JOIN users ON users.id = favorite.user_id
            WHERE users.username=$username)""")

    val byTag: Option[Fragment] =
      maybeTagName.map(tagName => fr"""
          articles.id IN (
            SELECT articles_tags.article_id FROM articles_tags
            INNER JOIN tags ON tags.id = articles_tags.tag_id
            WHERE tags.name=$tagName)""")

    sql"""
      SELECT articles.id, articles.slug, articles.title, articles.description, articles.body, articles.created_at, articles.updated_at, articles.author_id
      FROM articles
      INNER JOIN users ON articles.author_id = users.user_id
      INNER JOIN favorite ON articles.id = favorite.favorited_id AND users.id = favorite.user_id
      """.stripMargin

    (fr"""
      SELECT articles.id, articles.slug, articles.title, articles.description, articles.body, articles.created_at, articles.updated_at, articles.author_id
      FROM articles""" ++ whereOrOpt(byAuthor, byFavoritedAuthor, byTag) ++
    fr"ORDER BY articles.created_at ASC LIMIT ${pagination.limit} OFFSET ${pagination.offset}").query[Article]
  }

  def listArticlesByFavoritedUsers(userId: UserId, pagination: Pagination): doobie.Query0[Article] =
    sql"""
      SELECT articles.id, articles.slug, articles.title, articles.description, articles.body, articles.created_at, articles.updated_at, articles.author_id
      FROM articles WHERE articles.author_id IN
      (
        SELECT followers.followee_id FROM followers
        INNER JOIN users ON users.id = followers.follower_id
        WHERE users.id = $userId
      )
      ORDER BY articles.created_at ASC LIMIT ${pagination.limit} OFFSET ${pagination.offset}
      """.query[Article]

}
