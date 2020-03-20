package com.real.world.http4s.quill

import com.real.world.http4s.model.comment.Comment.CommentId
import com.real.world.http4s.quill.DoobiePostgresContext._
import com.real.world.http4s.model.comment.Comment

import doobie.syntax.connectionio.toConnectionIOOps
import io.getquill.{ idiom => _ }

import cats.effect.IO

object Comments {

  private val comments = quote {
    querySchema[Comment](
      "comments",
      _.id -> "id",
      _.body -> "body",
      _.articleId -> "article_id",
      _.authorId -> "author_id",
      _.createdAt -> "created_at",
      _.updatedAt -> "updated_at"
    )
  }

  def findById(commentId: CommentId)(implicit xa: doobie.Transactor[IO]): IO[Option[Comment]] =
    run(quote(comments.filter(_.id == lift(commentId)))).map(_.headOption).transact(xa)

}
