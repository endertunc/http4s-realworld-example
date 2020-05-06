package com.real.world.http4s.service

import cats.data.NonEmptyList
import cats.effect.IO
import com.real.world.http4s.AppError.ArticleNotFound
import com.real.world.http4s.RealWorldApp
import com.real.world.http4s.generators.ArticleGenerator
import com.real.world.http4s.model.article.IsFavorited.{ Favorited, NotFavorited }
import com.real.world.http4s.model.{ FavoritesCount, Pagination }
import com.real.world.http4s.quill.Articles
import org.scalatest.OptionValues
import org.scalatest.flatspec.AsyncFlatSpec
import eu.timepit.refined.auto._

class ArticleServiceSpec extends AsyncFlatSpec with RealWorldApp with OptionValues {

  "Article service" should "should create article" in IOSuit {
    for {
      persistedUser <- insertUser()
      article = ArticleGenerator.generateCreateArticleRequest
      (persistedArticle, persistedTags) <- ctx.articleService.createArticle(
        article,
        persistedUser.id
      )
      _ = persistedTags should have size article.tagList.value.size
      retrievedTags    <- ctx.tagService.findTagsByArticleId(persistedArticle.id)
      retrievedArticle <- ctx.articleService.findArticleBySlug(persistedArticle.slug)
    } yield {
      retrievedTags should have size article.tagList.value.size
      retrievedArticle.id shouldBe persistedArticle.id
    }
  }

  it should "find article by slug" in IOSuit {
    for {
      persistedUser    <- insertUser()
      persistedArticle <- Articles.insertArticle(persistedUser.id)
      retrievedArticle <- ctx.articleService.findArticleBySlug(persistedArticle.slug)
    } yield retrievedArticle.slug shouldBe persistedArticle.slug
  }

  it should "fail with ArticleNotFound when user attempts to find an article that does not exist" in FailedIOSuit {
    val unpersistedArticle = ArticleGenerator.generateArticle()
    for {
      _ <- ctx.articleService.findArticleBySlug(unpersistedArticle.slug)
    } yield fail("Non-existing article found")
  }(_ shouldBe a[ArticleNotFound])

  // add comment + tags to make sure that they are also deleted
  it should "delete article by articleId and userId" in IOSuit {
    for {
      persistedUser    <- insertUser()
      persistedArticle <- Articles.insertArticle(persistedUser.id)
      retrievedArticle <- ctx.articleService.findArticleBySlug(persistedArticle.slug)
      _                <- ctx.articleService.deleteArticleBySlugAndUserId(retrievedArticle.slug, persistedUser.id)
      articleAsOpt     <- Articles.findBySlug(retrievedArticle.slug)
    } yield articleAsOpt should not be defined
  }

  it should "fail with ArticleNotFound when unauthorized user attempts to delete an article" in FailedIOSuit {
    for {
      ownerUser        <- insertUser()
      unauthorizedUser <- insertUser()
      persistedArticle <- Articles.insertArticle(ownerUser.id)
      retrievedArticle <- ctx.articleService.findArticleBySlug(persistedArticle.slug)
      _                <- ctx.articleService.deleteArticleBySlugAndUserId(retrievedArticle.slug, unauthorizedUser.id)
    } yield fail("An article deleted by unauthorized user")
  }(_ shouldBe a[ArticleNotFound])

  it should "favorite article by slug" in IOSuit {
    for {
      persistedUser       <- insertUser()
      persistedArticle    <- Articles.insertArticle(persistedUser.id)
      _                   <- ctx.articleService.favoriteArticleBySlug(persistedArticle.slug, persistedUser.id)
      favoriteRecordAsOpt <- Articles.findFavorited(persistedUser.id, persistedArticle.id)
    } yield favoriteRecordAsOpt shouldBe defined
  }

  it should "fail with ArticleNotFound when user attempts to favorite non-existing article" in FailedIOSuit {
    for {
      persistedUser <- insertUser()
      unpersistedArticle = ArticleGenerator.generateArticle()
      _ <- ctx.articleService.favoriteArticleBySlug(unpersistedArticle.slug, persistedUser.id)
    } yield fail("Non-existing article favorited")
  }(_ shouldBe a[ArticleNotFound])

  it should "unfavorite article by slug" in IOSuit {
    for {
      persistedUser       <- insertUser()
      persistedArticle    <- Articles.insertArticle(persistedUser.id)
      _                   <- Articles.favoriteArticle(persistedUser.id, persistedArticle.id)
      favoritedCheck      <- Articles.findFavorited(persistedUser.id, persistedArticle.id)
      _                   <- IO(favoritedCheck shouldBe defined)
      _                   <- ctx.articleService.unfavoriteArticleBySlug(persistedArticle.slug, persistedUser.id)
      favoriteRecordAsOpt <- Articles.findFavorited(persistedUser.id, persistedArticle.id)
    } yield favoriteRecordAsOpt should not be defined
  }

  it should "list articles by author" in IOSuit {
    for {
      persistedUser1    <- insertUser()
      persistedUser2    <- insertUser()
      persistedUser3    <- insertUser()
      persistedArticle1 <- Articles.insertArticle(persistedUser1.id)
      persistedArticle2 <- Articles.insertArticle(persistedUser1.id)
      _                 <- Articles.insertArticle(persistedUser2.id)
      _                 <- Articles.insertArticle(persistedUser3.id)
      receivedArticles <- ctx.articleService.findAll(
        Some(persistedUser1.username),
        None,
        None,
        None,
        defaultPagination
      )

    } yield List(persistedArticle1, persistedArticle2) should contain theSameElementsInOrderAs receivedArticles.map(_._1)

  }

  it should "list articles by favorited author" in IOSuit {
    for {
      persistedUser1    <- insertUser()
      persistedUser2    <- insertUser()
      persistedUser3    <- insertUser()
      persistedArticle1 <- Articles.insertArticle(persistedUser1.id)
      _                 <- Articles.insertArticle(persistedUser1.id)
      _                 <- Articles.insertArticle(persistedUser2.id)
      _                 <- Articles.insertArticle(persistedUser3.id)
      _                 <- Articles.favoriteArticle(persistedUser3.id, persistedArticle1.id)
      receivedArticles <- ctx.articleService.findAll(
        None,
        Some(persistedUser3.username),
        None,
        None,
        defaultPagination
      )
    } yield List(persistedArticle1) should contain theSameElementsInOrderAs receivedArticles.map(_._1)
  }

  it should "apply pagination rules while listing articles" in IOSuit {
    for {
      persistedUser1    <- insertUser()
      persistedUser2    <- insertUser()
      persistedUser3    <- insertUser()
      persistedArticle1 <- Articles.insertArticle(persistedUser1.id)
      persistedArticle2 <- Articles.insertArticle(persistedUser1.id)
      persistedArticle3 <- Articles.insertArticle(persistedUser2.id)
      persistedArticle4 <- Articles.insertArticle(persistedUser2.id)
      _                 <- Articles.favoriteArticle(persistedUser3.id, persistedArticle3.id)
      _                 <- Articles.favoriteArticle(persistedUser3.id, persistedArticle4.id)
      receivedArticles <- ctx.articleService.findAll(
        Some(persistedUser1.username),
        Some(persistedUser3.username),
        None,
        None,
        Pagination(limit = 2, offset = 1)
      )
    } yield List(persistedArticle2, persistedArticle3) should contain theSameElementsInOrderAs receivedArticles.map(_._1)

  }

  it should "list articles by favorited users (feed)" in IOSuit {
    for {
      persistedUser1    <- insertUser()
      persistedUser2    <- insertUser()
      persistedUser3    <- insertUser()
      persistedUser4    <- insertUser()
      persistedArticle1 <- Articles.insertArticle(persistedUser1.id)
      persistedArticle2 <- Articles.insertArticle(persistedUser1.id)
      persistedArticle3 <- Articles.insertArticle(persistedUser2.id)
      persistedArticle4 <- Articles.insertArticle(persistedUser2.id)
      persistedArticle5 <- Articles.insertArticle(persistedUser3.id)
      persistedArticle6 <- Articles.insertArticle(persistedUser4.id)
      _                 <- ctx.profileService.follow(persistedUser2.username, persistedUser1.id)
      _                 <- ctx.profileService.follow(persistedUser4.username, persistedUser1.id)
      receivedArticles  <- ctx.articleService.feed(persistedUser1.id, defaultPagination)

    } yield List(persistedArticle3, persistedArticle4, persistedArticle6) should contain theSameElementsInOrderAs receivedArticles.map(_._1)
  }

  it should "apply pagination rules while listing articles by favorited users (feed)" in IOSuit {
    for {
      persistedUser1    <- insertUser()
      persistedUser2    <- insertUser()
      persistedUser3    <- insertUser()
      persistedUser4    <- insertUser()
      persistedArticle1 <- Articles.insertArticle(persistedUser1.id)
      persistedArticle2 <- Articles.insertArticle(persistedUser1.id)
      persistedArticle3 <- Articles.insertArticle(persistedUser2.id)
      persistedArticle4 <- Articles.insertArticle(persistedUser2.id)
      persistedArticle5 <- Articles.insertArticle(persistedUser3.id)
      persistedArticle6 <- Articles.insertArticle(persistedUser4.id)
      _                 <- ctx.profileService.follow(persistedUser2.username, persistedUser1.id)
      _                 <- ctx.profileService.follow(persistedUser4.username, persistedUser1.id)
      receivedArticles  <- ctx.articleService.feed(persistedUser1.id, Pagination(limit = 1, offset = 1))
    } yield {
      List(persistedArticle4) should contain theSameElementsInOrderAs receivedArticles.map(_._1)
    }

  }

  it should "return Favorited when article is favorited by the user" in IOSuit {
    for {
      persistedUser    <- insertUser()
      persistedArticle <- Articles.insertArticle(persistedUser.id)
      _                <- ctx.articleService.favoriteArticleBySlug(persistedArticle.slug, persistedUser.id)
      isFavorited      <- ctx.articleService.isFavoritedByUser(persistedArticle.id, persistedUser.id)
    } yield isFavorited shouldBe Favorited

  }

  it should "return NotFavorited when article is not favorited by the user" in IOSuit {
    for {
      persistedUser    <- insertUser()
      persistedArticle <- Articles.insertArticle(persistedUser.id)
      isFavorited      <- ctx.articleService.isFavoritedByUser(persistedArticle.id, persistedUser.id)
    } yield isFavorited shouldBe NotFavorited
  }

  it should "return FavoritedCount for an article" in IOSuit {
    for {
      persistedUser1       <- insertUser()
      persistedUser2       <- insertUser()
      persistedUser3       <- insertUser()
      persistedArticle     <- Articles.insertArticle(persistedUser1.id)
      favoritedCountBefore <- ctx.articleService.findFavoritedCount(persistedArticle.id)
      _ = favoritedCountBefore shouldBe FavoritesCount(0)
      _                   <- ctx.articleService.favoriteArticleBySlug(persistedArticle.slug, persistedUser2.id)
      _                   <- ctx.articleService.favoriteArticleBySlug(persistedArticle.slug, persistedUser3.id)
      favoritedCountAfter <- ctx.articleService.findFavoritedCount(persistedArticle.id)
    } yield favoritedCountAfter shouldBe FavoritesCount(2)
  }

  it should "update article" in IOSuit {
    for {
      persistedUser    <- insertUser()
      persistedArticle <- Articles.insertArticle(persistedUser.id)
      updatedArticleRequest = ArticleGenerator.generateUpdateArticleRequest
      updatedArticle <- ctx.articleService.updateArticle(updatedArticleRequest, persistedArticle.slug, persistedUser.id)
    } yield {
      updatedArticle.title shouldBe updatedArticleRequest.title.value
      updatedArticle.description shouldBe updatedArticleRequest.description.value
      updatedArticle.body shouldBe updatedArticleRequest.body.value
      updatedArticle.authorId shouldBe persistedUser.id
      updatedArticle.slug should not be persistedArticle.slug
    }
  }

  it should "fail with ArticleNotFound when unauthorized user attempts to update article" in FailedIOSuit {
    for {
      ownerUser        <- insertUser()
      unauthorizedUser <- insertUser()
      persistedArticle <- Articles.insertArticle(ownerUser.id)
      updatedArticleRequest = ArticleGenerator.generateUpdateArticleRequest
      _ <- ctx.articleService.updateArticle(updatedArticleRequest, persistedArticle.slug, unauthorizedUser.id)
    } yield fail("Unauthorized user updated an article")
  }(_ shouldBe a[ArticleNotFound])

  it should "find favorited articles by userId" in IOSuit {
    for {
      persistedUser     <- insertUser()
      author1           <- insertUser()
      author2           <- insertUser()
      persistedArticle1 <- Articles.insertArticle(author1.id)
      persistedArticle2 <- Articles.insertArticle(author1.id)
      persistedArticle3 <- Articles.insertArticle(author2.id)
      persistedArticle4 <- Articles.insertArticle(author2.id)
      beforeFavorited <- ctx.articleService.findFavoritedsByUserId(
        NonEmptyList.of(persistedArticle1.id, persistedArticle2.id, persistedArticle3.id, persistedArticle4.id),
        Some(persistedUser.id)
      )
      _ <- IO(beforeFavorited shouldBe empty)
      _ <- ctx.articleService.favoriteArticleBySlug(persistedArticle1.slug, persistedUser.id)
      _ <- ctx.articleService.favoriteArticleBySlug(persistedArticle3.slug, persistedUser.id)
      favoritedArticleIds <- ctx.articleService.findFavoritedsByUserId(
        NonEmptyList.of(persistedArticle1.id, persistedArticle2.id, persistedArticle3.id, persistedArticle4.id),
        Some(persistedUser.id)
      )
    } yield favoritedArticleIds should contain theSameElementsAs List(persistedArticle1.id, persistedArticle3.id)
  }

}
