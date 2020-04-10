package com.real.world.http4s.service

import cats.effect.Async
import cats.implicits._

import com.real.world.http4s.model._
import com.real.world.http4s.model.comment.Comment
import com.real.world.http4s.model.comment.Comment.CommentId
import com.real.world.http4s.model.profile.Profile
import com.real.world.http4s.model.user.User
import com.real.world.http4s.repository.algebra.CommentRepositoryAlgebra

import io.chrisdavenport.log4cats.Logger

class CommentService[F[_]: Async: Logger]()(implicit articleService: ArticleService[F], commentsRepositoryAlgebra: CommentRepositoryAlgebra[F]) {

  def createComment(commentBody: CommentBody, slug: Slug, userId: UserId): F[Comment] =
    for {
      _       <- Logger[F].trace(s"Creating comment with body/data [$commentBody], slug [$slug] and user [$userId]")
      article <- articleService.findArticleBySlug(slug)
      comment <- commentsRepositoryAlgebra.createComment(commentBody, article.id, userId)
    } yield comment

  def findCommentsWithAuthorByArticleId(articleId: ArticleId): F[List[(Comment, User)]] =
    for {
      _                  <- Logger[F].trace(s"Retrieving comments for article [$articleId]")
      commentsWithAuthor <- commentsRepositoryAlgebra.findCommentsWithAuthorByArticleId(articleId)
      _                  <- Logger[F].trace(s"Found [${commentsWithAuthor.length}] comment(s) for article [$articleId]")
    } yield commentsWithAuthor

  def findCommentsWithAuthorProfile(articleId: ArticleId, userId: Option[UserId]): F[List[(Comment, Profile)]] =
    for {
      _                   <- Logger[F].trace(s"Retrieving comments with author profile for article [$articleId] and user [$userId]")
      commentsWithProfile <- commentsRepositoryAlgebra.findCommentsWithAuthor(articleId, userId)
      _                   <- Logger[F].trace(s"Found [${commentsWithProfile.length}] comment(s) for article [$articleId]")
    } yield commentsWithProfile

  def deleteByCommentIdAndAuthorId(commentId: CommentId, authorId: UserId): F[Unit] =
    for {
      _ <- Logger[F].trace(s"User [$authorId] is trying to delete comment [$commentId]")
      _ <- commentsRepositoryAlgebra.deleteByCommentIdAndAuthorId(commentId, authorId)
      _ <- Logger[F].trace(s"User [$authorId] successfully deleted comment [$commentId]")
    } yield ()

}

object CommentService {
  def apply[F[_]: Async: Logger]()(
      implicit articleService: ArticleService[F],
      commentsRepositoryAlgebra: CommentRepositoryAlgebra[F]
  ): CommentService[F] =
    new CommentService()
}
