package com.real.world.http4s.model.comment

import java.time.Instant

import com.real.world.http4s.model.article.Article.ArticleId
import Comment.{ CommentBody, CommentId }
import com.real.world.http4s.model.user.User.UserId

import io.circe.Codec
import io.circe.generic.extras.semiauto.deriveUnwrappedCodec
import io.circe.generic.semiauto.deriveCodec

// ToDo dont use coded everywhere!

final case class Comment(id: CommentId, body: CommentBody, articleId: ArticleId, authorId: UserId, createdAt: Instant, updatedAt: Instant)

object Comment {

  case class CommentId(value: Int) extends AnyVal
  case class CommentBody(value: String) extends AnyVal

  implicit val CommentIdCodec: Codec[CommentId]     = deriveUnwrappedCodec[CommentId]
  implicit val CommentBodyCodec: Codec[CommentBody] = deriveUnwrappedCodec[CommentBody]

  implicit val CommentEncoder: Codec[Comment] = deriveCodec[Comment]

}
