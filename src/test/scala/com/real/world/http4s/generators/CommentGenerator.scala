package com.real.world.http4s.generators

import java.time.Instant

import cats.effect.IO

import com.real.world.http4s.authentication.PasswordHasher
import com.real.world.http4s.model.comment
import com.real.world.http4s.model.comment.{ Comment, CreateCommentInput, CreateCommentInputWrapper, CreateCommentRequest }

import org.scalacheck.Gen

object CommentGenerator extends GeneratorsBase {

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

  private def createCommentInputGen: Gen[CreateCommentInput] =
    for {
      commentBody <- commentBodyGen
    } yield CreateCommentInput(
      body = commentBody.value.value
    )

  private def createCommentRequestGen: Gen[CreateCommentRequest] =
    for {
      commentBody <- commentBodyGen
    } yield CreateCommentRequest(
      body = commentBody
    )

  def generateComment()(implicit tsecPasswordHasher: PasswordHasher[IO]): Comment = commentGen.sample.get
  def generateCreateCommentInputWrapper: CreateCommentInputWrapper                = CreateCommentInputWrapper(createCommentInputGen.sample.get)
  def generateCreateCommentRequest: CreateCommentRequest                          = createCommentRequestGen.sample.get

}
