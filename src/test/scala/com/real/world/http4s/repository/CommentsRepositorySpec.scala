package com.real.world.http4s.repository

import cats.effect.IO

import doobie.scalatest.IOChecker

import com.real.world.http4s.base.ServicesAndRepos
import com.real.world.http4s.generators.{ CommentGenerator, UserGenerator }

import org.scalatest.flatspec.AnyFlatSpec

class CommentsRepositorySpec extends AnyFlatSpec with IOChecker with ServicesAndRepos {

  private val comment                            = CommentGenerator.generateComment
  private val user                               = UserGenerator.generateUser
  override def transactor: doobie.Transactor[IO] = xa

  "CommentStatements" should "compile" in {
//    check(CommentStatement.createComment[IO](comment.body, comment.articleId, comment.authorId).unsafeRunSync) // ToDo better way to handle this
//    check(CommentStatement.deleteByCommentIdAuthorId(comment.id, user.id))
    check(CommentStatement.findCommentsWithAuthorByArticleId(comment.articleId))
//    check(CommentStatement.deleteCommentsByArticleId(comment.articleId))
  }

}
