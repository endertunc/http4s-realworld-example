package com.real.world.http4s.quill

import cats.effect.IO

import doobie.syntax.connectionio.toConnectionIOOps

import com.real.world.http4s.authentication.PasswordHasher
import com.real.world.http4s.generators.ArticleGenerator
import com.real.world.http4s.model.article.Article
import com.real.world.http4s.model.{ ArticleId, Slug, UserId }
import com.real.world.http4s.quill.DoobiePostgresContext._
import com.real.world.http4s.repository.QuillSupport

import io.getquill.{ idiom => _ }

object Articles extends QuillSupport {
  case class Favorite(id: Int, userId: UserId, articleId: ArticleId)

  implicit private val articleInsertMeta  = insertMeta[Article](_.id)
  implicit private val favoriteInsertMeta = insertMeta[Favorite](_.id)

  private val articles = quote {
    querySchema[Article](
      "articles",
      _.id -> "id",
      _.slug -> "slug",
      _.title -> "title",
      _.description -> "description",
      _.body -> "body",
      _.createdAt -> "created_at",
      _.updatedAt -> "updated_at",
      _.authorId -> "author_id"
    )
  }

  private val favorites = quote {
    querySchema[Favorite](
      "favorite",
      _.articleId -> "id",
      _.userId -> "user_id",
      _.articleId -> "favorited_id"
    )
  }

  def findBySlug(slug: Slug)(implicit xa: doobie.Transactor[IO]): IO[Option[Article]] =
    run(quote(articles.filter(_.slug == lift(slug)))).map(_.headOption).transact(xa)

  def insertArticle(userId: UserId)(implicit xa: doobie.Transactor[IO], tsecPasswordHasher: PasswordHasher[IO]): IO[Article] = {
    val article = ArticleGenerator.generateArticle.copy(authorId = userId)
    run(quote(articles.insert(lift(article)).returning(article => article))).transact(xa)
  }

  def findFavorited(userId: UserId, articleId: ArticleId)(implicit xa: doobie.Transactor[IO]): IO[Option[Favorite]] =
    run(quote(favorites.filter(favorite => favorite.userId == lift(userId) && favorite.articleId == lift(articleId))))
      .transact(xa)
      .map(_.headOption)

  def favoriteArticle(userId: UserId, articleId: ArticleId)(implicit xa: doobie.Transactor[IO]): IO[Favorite] =
    run(quote(favorites.insert(lift(Favorite(0, userId, articleId))).returning(favorite => favorite)))
      .transact(xa)

}
