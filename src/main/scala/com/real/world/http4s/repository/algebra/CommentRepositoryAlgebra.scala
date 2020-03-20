package com.real.world.http4s.repository.algebra

import com.real.world.http4s.model.comment.Comment.{ CommentBody, CommentId }
import com.real.world.http4s.model.article.Article.ArticleId
import com.real.world.http4s.model.comment.Comment
import com.real.world.http4s.model.user.User
import com.real.world.http4s.model.user.User.UserId
import com.real.world.http4s.model.comment.Comment
import com.real.world.http4s.model.profile.Profile
import com.real.world.http4s.model.user.User

trait CommentRepositoryAlgebra[F[_]] {

  def deleteByCommentIdAndAuthorId(commentId: CommentId, authorId: UserId): F[Unit]
  def findCommentsWithAuthorByArticleId(articleId: ArticleId): F[List[(Comment, User)]]
  def createComment(commentBody: CommentBody, articleId: ArticleId, authorId: UserId): F[Comment]
  def findCommentsWithAuthor(articleId: ArticleId, userId: Option[UserId]): F[List[(Comment, Profile)]]

}
