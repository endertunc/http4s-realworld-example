package com.real.world.http4s.repository

import cats.data.NonEmptyList
import cats.effect.Async
import cats.implicits._
import com.colisweb.tracing.core.TracingContext
import doobie._
import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.update.Update
import com.real.world.http4s.model.NewTypeImplicits._
import com.real.world.http4s.model._
import com.real.world.http4s.model.tag.Tag
import com.real.world.http4s.model.tag.Tag.{ TagId, TagName }
import com.real.world.http4s.repository.algebra.TagRepositoryAlgebra
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger

// ToDo values that might be related with errors
class PostgresTagRepositoryAlgebra[F[_]: Async]()(implicit L: SelfAwareStructuredLogger[F], xa: Transactor[F]) extends TagRepositoryAlgebra[F] {

  override def createTags(tags: List[TagName])(implicit tracingContext: TracingContext[F]): F[List[Tag]] =
    TagsStatement
      .insertTags(tags)
      .compile
      .toList
      .transact(xa)

  override def findAll()(implicit tracingContext: TracingContext[F]): F[List[Tag]] =
    TagsStatement.findAll
      .to[List]
      .transact(xa)

  // ToDo .map(_ => ())
  override def insertArticleTagsAssociation(articleId: ArticleId, tags: List[Tag])(implicit tracingContext: TracingContext[F]): F[Unit] =
    TagsStatement
      .insertArticleTagsAssociation(articleId, tags)
      .compile
      .toList
      .map(_ => ())
      .transact(xa)

  override def findTagsByArticleId(articleId: ArticleId)(implicit tracingContext: TracingContext[F]): F[List[Tag]] =
    TagsStatement
      .findTagsByArticleId(articleId)
      .to[List]
      .transact(xa)

  override def findTagsByArticleIds(articleIds: NonEmptyList[ArticleId])(implicit tracingContext: TracingContext[F]): F[Map[ArticleId, List[Tag]]] =
    TagsStatement
      .findTagsByArticleIds(articleIds)
      .to[List]
      .map { list =>
        list
          .groupBy {
            case (articleId, _) => articleId
          }
          .mapValues(_.map { case (_, tags) => tags })
      }
      .transact(xa)

}

object PostgresTagRepositoryAlgebra {
  def apply[F[_]: Async: SelfAwareStructuredLogger: Transactor](): PostgresTagRepositoryAlgebra[F] = new PostgresTagRepositoryAlgebra()
}

object TagsStatement {

  def findAll: doobie.Query0[Tag] =
    sql"SELECT id, name FROM tags".query[Tag]

  def insertTags(tags: List[TagName]): fs2.Stream[doobie.ConnectionIO, Tag] = {
    val sql = "INSERT INTO tags (name) values (?) ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name RETURNING id, name"
    Update[TagName](sql).updateManyWithGeneratedKeys[Tag]("id", "name")(tags)
  }

  // ToDo ignore on duplicate
  def insertArticleTagsAssociation(articleId: ArticleId, tags: List[Tag]): fs2.Stream[doobie.ConnectionIO, (Int, Int, Int)] = {
    val articleIdWithTagId: List[(ArticleId, TagId)] = tags.map(tag => articleId -> tag.id)
    val sql                                          = "INSERT INTO articles_tags (article_id, tag_id) values (?, ?)"
    Update[(ArticleId, TagId)](sql).updateManyWithGeneratedKeys[(Int, Int, Int)]("id", "article_id", "tag_id")(articleIdWithTagId)
  }

  def findTagsByArticleId(articleId: ArticleId): doobie.Query0[Tag] = sql"""
    SELECT tags.id, tags.name FROM tags
    INNER JOIN articles_tags ON articles_tags.tag_id = tags.id
    WHERE articles_tags.article_id = $articleId
    """.query[Tag]

  def findTagsByArticleIds(articleIds: NonEmptyList[ArticleId]): doobie.Query0[(ArticleId, Tag)] = (fr"""
    SELECT articles_tags.article_id, tags.id, tags.name FROM tags
    INNER JOIN articles_tags ON articles_tags.tag_id = tags.id
    WHERE""" ++ Fragments.in(fr"articles_tags.article_id", articleIds)).query[(ArticleId, Tag)]

  def deleteByArticleId(articleId: ArticleId): doobie.Update0 = sql"""DELETE FROM articles_tags WHERE articles_tags.article_id = $articleId""".update

}
