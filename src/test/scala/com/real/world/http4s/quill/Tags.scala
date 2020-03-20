package com.real.world.http4s.quill

import com.real.world.http4s.model.article.Article.ArticleId
import com.real.world.http4s.model.tag.Tag.TagId
import com.real.world.http4s.quill.DoobiePostgresContext._
import io.getquill.{ idiom => _ }

import cats.effect.IO

import doobie.syntax.connectionio.toConnectionIOOps

object Tags {

  case class ArticlesTags(id: Int, articleId: ArticleId, tagId: TagId)

  private val articlesTags = quote {
    querySchema[ArticlesTags](
      "articles_tags",
      _.id -> "id",
      _.articleId -> "article_id",
      _.tagId -> "tag_id"
    )
  }

  def findByArticleIdAndTagID(articleId: ArticleId, tagId: TagId)(
      implicit xa: doobie.Transactor[IO]
  ): IO[Option[ArticlesTags]] =
    run(quote(articlesTags.filter(record => record.articleId == lift(articleId) && record.tagId == lift(tagId))))
      .map(_.headOption)
      .transact(xa)

}
