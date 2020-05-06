package com.real.world.http4s.service

import cats.effect.Async
import cats.implicits._
import com.colisweb.tracing.core.TracingContext
import com.real.world.http4s.model._
import com.real.world.http4s.model.comment.Comment
import com.real.world.http4s.model.comment.Comment.CommentId
import com.real.world.http4s.model.profile.Profile
import com.real.world.http4s.model.user.User
import com.real.world.http4s.repository.algebra.CommentRepositoryAlgebra
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger

class CommentService[F[_]: Async]()(
    implicit L: SelfAwareStructuredLogger[F],
    articleService: ArticleService[F],
    commentsRepositoryAlgebra: CommentRepositoryAlgebra[F]
) {

  def createComment(commentBody: CommentBody, slug: Slug, userId: UserId)(implicit tracingContext: TracingContext[F]): F[Comment] =
    for {
      _       <- L.trace(s"Creating comment with body/data [$commentBody], slug [$slug] and user [$userId]")
      article <- articleService.findArticleBySlug(slug)
      comment <- commentsRepositoryAlgebra.createComment(commentBody, article.id, userId)
    } yield comment

  def findCommentsWithAuthorByArticleId(articleId: ArticleId)(implicit tracingContext: TracingContext[F]): F[List[(Comment, User)]] =
    for {
      _                  <- L.trace(s"Retrieving comments for article [$articleId]")
      commentsWithAuthor <- commentsRepositoryAlgebra.findCommentsWithAuthorByArticleId(articleId)
      _                  <- L.trace(s"Found [${commentsWithAuthor.length}] comment(s) for article [$articleId]")
    } yield commentsWithAuthor

  def findCommentsWithAuthorProfile(articleId: ArticleId, userId: Option[UserId])(
      implicit tracingContext: TracingContext[F]
  ): F[List[(Comment, Profile)]] =
    for {
      _                   <- L.trace(s"Retrieving comments with author profile for article [$articleId] and user [$userId]")
      commentsWithProfile <- commentsRepositoryAlgebra.findCommentsWithAuthor(articleId, userId)
      _                   <- L.trace(s"Found [${commentsWithProfile.length}] comment(s) for article [$articleId]")
    } yield commentsWithProfile

  def deleteByCommentIdAndAuthorId(commentId: CommentId, authorId: UserId)(implicit tracingContext: TracingContext[F]): F[Unit] =
    for {
      _ <- L.trace(s"User [$authorId] is trying to delete comment [$commentId]")
      _ <- commentsRepositoryAlgebra.deleteByCommentIdAndAuthorId(commentId, authorId)
      _ <- L.trace(s"User [$authorId] successfully deleted comment [$commentId]")
    } yield ()

}

object CommentService {
  def apply[F[_]: Async: SelfAwareStructuredLogger]()(
      implicit articleService: ArticleService[F],
      commentsRepositoryAlgebra: CommentRepositoryAlgebra[F]
  ): CommentService[F] =
    new CommentService()
}
