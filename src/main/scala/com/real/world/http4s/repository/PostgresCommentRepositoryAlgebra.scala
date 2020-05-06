package com.real.world.http4s.repository

import java.time.Instant

import cats.data.NonEmptyList
import cats.effect.Async
import cats.free.Free
import cats.implicits._
import com.colisweb.tracing.core.TracingContext
import doobie._
import doobie.free.connection
import doobie.implicits._
import doobie.implicits.legacy.instant.JavaTimeInstantMeta
import doobie.refined.implicits._
import doobie.refined.implicits._
import com.real.world.http4s.AppError.RecordNotFound
import com.real.world.http4s.model.NewTypeImplicits._
import com.real.world.http4s.model._
import com.real.world.http4s.model.comment.Comment
import com.real.world.http4s.model.comment.Comment.CommentId
import com.real.world.http4s.model.profile.{ IsFollowing, Profile }
import com.real.world.http4s.model.user.User
import com.real.world.http4s.repository.algebra.CommentRepositoryAlgebra
import eu.timepit.refined.auto._
import io.chrisdavenport.log4cats.{ Logger, SelfAwareStructuredLogger }

class PostgresCommentRepositoryAlgebra[F[_]: Async]()(implicit L: SelfAwareStructuredLogger[F], xa: Transactor[F])
    extends CommentRepositoryAlgebra[F] {

  // ToDo what if there is no comment with that Id?
  override def deleteByCommentIdAndAuthorId(commentId: CommentId, authorId: UserId)(implicit tracingContext: TracingContext[F]): F[Unit] =
    CommentStatement
      .deleteByCommentIdAuthorId(commentId, authorId)
      .transact(xa)
      .flatMap {
        case effectedRows if effectedRows == 0 =>
          RecordNotFound(s"Comment is not found")
            .raiseError[F, Unit] <* Logger[F].warn(
            s"Comment [$commentId] does not exist or user [$authorId] is not the author"
          )
        case effectedRows if effectedRows == 1 => ().pure[F]
      }

  override def createComment(commentBody: CommentBody, articleId: ArticleId, authorId: UserId)(
      implicit tracingContext: TracingContext[F]
  ): F[Comment] =
    for {
      query   <- CommentStatement.createComment[F](commentBody, articleId, authorId)
      comment <- query.transact(xa)
    } yield comment

  override def findCommentsWithAuthorByArticleId(articleId: ArticleId)(implicit tracingContext: TracingContext[F]): F[List[(Comment, User)]] =
    CommentStatement
      .findCommentsWithAuthorByArticleId(articleId)
      .to[List]
      .transact(xa)

  def findCommentsWithAuthor(articleId: ArticleId, userId: Option[UserId])(
      implicit tracingContext: TracingContext[F]
  ): F[List[(Comment, Profile)]] = {
    val query: Free[connection.ConnectionOp, List[(Comment, Profile)]] = for {
      commentsAndUsers <- CommentStatement
        .findCommentsWithAuthorByArticleId(articleId)
        .to[List]
      followees <- {
        (NonEmptyList.fromList(commentsAndUsers.map(_._2.id)), userId).bisequence
          .map {
            case (nelOfUserIds, userId) => FollowersStatement.findFollowees(nelOfUserIds, userId).to[Set]
          }
          .getOrElse(Set.empty[UserId].pure[doobie.ConnectionIO])
      }
    } yield commentsAndUsers map {
      case (comment, user) =>
        val isFollowing = IsFollowing.fromBoolean(followees.contains(user.id))
        (comment, Profile(user.username, user.bio, user.image, isFollowing))
    }
    query.transact(xa)

  }
}

object PostgresCommentRepositoryAlgebra {
  def apply[F[_]: Async: SelfAwareStructuredLogger: Transactor](): CommentRepositoryAlgebra[F] = new PostgresCommentRepositoryAlgebra()
}

object CommentStatement {

  import QuillSupport.DoobiePostgresContext._
  import QuillSupport._

  implicit private val commentInsertMeta = insertMeta[Comment](_.id)

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

  def createComment[F[_]: Async](commentBody: CommentBody, articleId: ArticleId, authorId: UserId): F[doobie.ConnectionIO[Comment]] =
    Async[F]
      .delay(Instant.now)
      .map(now => run(quote(comments.insert(lift(Comment(CommentId(1), commentBody, articleId, authorId, now, now))).returning(comment => comment))))

  def findCommentsWithAuthorByArticleId(articleId: ArticleId): doobie.Query0[(Comment, User)] =
    sql"""
          SELECT
          c.id, c.body, c.article_id, c.author_id, c.created_at, c.updated_at,
          u.id, u.email, u.password, u.username, u.bio, u.image
          FROM comments c
          INNER JOIN users u ON c.author_id = u.id
          WHERE c.article_id=$articleId""".query[(Comment, User)]

  def deleteByCommentIdAuthorId(commentId: CommentId, authorId: UserId): doobie.ConnectionIO[Long] =
    run(quote(comments.filter(comment => comment.id == lift(commentId) && comment.authorId == lift(authorId))).delete)

  def deleteCommentsByArticleId(articleId: ArticleId): doobie.ConnectionIO[Long] =
    run(quote(comments.filter(_.articleId == lift(articleId))).delete)

}
