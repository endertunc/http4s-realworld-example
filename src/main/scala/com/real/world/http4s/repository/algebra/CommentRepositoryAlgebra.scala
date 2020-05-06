package com.real.world.http4s.repository.algebra

import com.colisweb.tracing.core.TracingContext
import com.real.world.http4s.model._
import com.real.world.http4s.model.comment.Comment
import com.real.world.http4s.model.comment.Comment.CommentId
import com.real.world.http4s.model.profile.Profile
import com.real.world.http4s.model.user.User

trait CommentRepositoryAlgebra[F[_]] {

  def deleteByCommentIdAndAuthorId(commentId: CommentId, authorId: UserId)(implicit tracingContext: TracingContext[F]): F[Unit]
  def findCommentsWithAuthorByArticleId(articleId: ArticleId)(implicit tracingContext: TracingContext[F]): F[List[(Comment, User)]]
  def createComment(commentBody: CommentBody, articleId: ArticleId, authorId: UserId)(implicit tracingContext: TracingContext[F]): F[Comment]
  def findCommentsWithAuthor(articleId: ArticleId, userId: Option[UserId])(implicit tracingContext: TracingContext[F]): F[List[(Comment, Profile)]]

}
