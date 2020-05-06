package com.real.world.http4s.service

import com.real.world.http4s.AppError.{ ArticleNotFound, RecordNotFound }
import com.real.world.http4s.RealWorldApp
import com.real.world.http4s.generators.{ ArticleGenerator, CommentGenerator }
import com.real.world.http4s.model.article.Article
import com.real.world.http4s.model.comment.Comment
import com.real.world.http4s.quill.Comments
import org.scalatest.flatspec.AsyncFlatSpec

class CommentServiceSpec extends AsyncFlatSpec with RealWorldApp {

  "CommentService" should "add comment to article" in IOSuit {
    val comment: Comment = CommentGenerator.generateComment
    for {
      persistedUser         <- insertUser()
      (persistedArticle, _) <- insertArticle(persistedUser.id)
      persistedComment      <- ctx.commentService.createComment(comment.body, persistedArticle.slug, persistedUser.id)
      retrievedComment      <- Comments.findById(persistedComment.id)
    } yield retrievedComment shouldBe defined
  }

  it should "fail with ArticleNotFound when attempt to add a comment to a non-existing article" in FailedIOSuit {
    val article: Article = ArticleGenerator.generateArticle
    val comment: Comment = CommentGenerator.generateComment
    for {
      persistedUser <- insertUser()
      _             <- ctx.commentService.createComment(comment.body, article.slug, persistedUser.id)
    } yield fail("Added a comment to non-existing article")
  }(_ shouldBe a[ArticleNotFound])

  it should "delete comment by commentId and userId" in IOSuit {
    val comment: Comment = CommentGenerator.generateComment
    for {
      persistedUser         <- insertUser()
      (persistedArticle, _) <- insertArticle(persistedUser.id)
      persistedComment      <- ctx.commentService.createComment(comment.body, persistedArticle.slug, persistedUser.id)
      _                     <- ctx.commentService.deleteByCommentIdAndAuthorId(persistedComment.id, persistedUser.id)
      retrievedComment      <- Comments.findById(persistedComment.id)
    } yield retrievedComment should not be defined
  }

  it should "fail with RecordDoesNotExist when unauthorized user attempt to delete a comment" in FailedIOSuit {
    val comment: Comment = CommentGenerator.generateComment
    for {
      persistedUser             <- insertUser()
      unauthorizedPersistedUser <- insertUser()
      (persistedArticle, _)     <- insertArticle(persistedUser.id)
      persistedComment          <- ctx.commentService.createComment(comment.body, persistedArticle.slug, persistedUser.id)
      _                         <- ctx.commentService.deleteByCommentIdAndAuthorId(persistedComment.id, unauthorizedPersistedUser.id)
    } yield fail("Unauthorized user was able to delete a comment")
  }(_ shouldBe a[RecordNotFound])

  it should "fail with RecordDoesNotExist when user attempt to delete a comment that does not exist" in FailedIOSuit {
    val comment: Comment = CommentGenerator.generateComment
    for {
      persistedUser <- insertUser()
      _             <- ctx.commentService.deleteByCommentIdAndAuthorId(comment.id, persistedUser.id)
    } yield fail("Non-existing comment deleted")
  }(_ shouldBe a[RecordNotFound])

  it should "retrieve (comment, user) by slug" in IOSuit {
    val comment1: Comment = CommentGenerator.generateComment
    val comment2: Comment = CommentGenerator.generateComment
    for {
      persistedUser         <- insertUser()
      (persistedArticle, _) <- insertArticle(persistedUser.id)
      _                     <- ctx.commentService.createComment(comment1.body, persistedArticle.slug, persistedUser.id)
      _                     <- ctx.commentService.createComment(comment2.body, persistedArticle.slug, persistedUser.id)
      retrievedComments     <- ctx.commentService.findCommentsWithAuthorByArticleId(persistedArticle.id)
    } yield retrievedComments should have size 2
  }

  it should "retrieve zero (comment,user) when topic does not exist" in IOSuit {
    val unpersistedArticle = ArticleGenerator.generateArticle()
    for {
      retrievedComments <- ctx.commentService.findCommentsWithAuthorByArticleId(unpersistedArticle.id)
    } yield retrievedComments should have size 0
  }

}
