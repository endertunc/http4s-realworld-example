package com.real.world.http4s.generators

import java.time.Instant

import com.real.world.http4s.model.comment.{ Comment, CreateComment, CreateCommentWrapper }
import com.real.world.http4s.model.comment
import com.real.world.http4s.model.comment.{ Comment, CreateComment, CreateCommentWrapper }
import com.real.world.http4s.security.PasswordHasher
import org.scalacheck.Gen

import cats.effect.IO

object CommentGenerator extends ValueClassGens {

  private def commentGen()(implicit tsecPasswordHasher: PasswordHasher[IO]): Gen[Comment] =
    for {
      commentId   <- commentIdGen
      commentBody <- commentBodyGen
      articleId   <- Gen.const(ArticleGenerator.generateArticle.id)
      authorId    <- Gen.const(UserGenerator.generateUser.id)
    } yield comment.Comment(
      id        = commentId,
      body      = commentBody,
      articleId = articleId,
      authorId  = authorId,
      createdAt = Instant.now,
      updatedAt = Instant.now
    )

  private def createCommentGen: Gen[CreateComment] =
    for {
      commentBody <- commentBodyGen
    } yield comment.CreateComment(
      body = commentBody
    )

  def generateComment()(implicit tsecPasswordHasher: PasswordHasher[IO]): Comment = commentGen.sample.get
  def generateCreateComment: CreateCommentWrapper =
    CreateCommentWrapper(createCommentGen.sample.get)

}
