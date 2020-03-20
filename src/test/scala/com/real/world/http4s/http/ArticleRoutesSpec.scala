package com.real.world.http4s.http

import com.real.world.http4s.AppError.ArticleNotFound
import com.real.world.http4s.base.ServicesAndRepos
import com.real.world.http4s.generators.{ ArticleGenerator, CommentGenerator }
import com.real.world.http4s.model.article.IsFavorited.NotFavorited
import com.real.world.http4s.model.article.IsFavorited.Favorited
import com.real.world.http4s.model.article._
import com.real.world.http4s.model.comment.{ CommentListResponseOutWrapper, CommentResponseWrapper, CreateComment, CreateCommentWrapper }
import com.real.world.http4s.model.profile.IsFollowing.NotFollowing
import com.real.world.http4s.quill.Articles
import com.real.world.http4s.AppError.ArticleNotFound
import com.real.world.http4s.base.ServicesAndRepos
import com.real.world.http4s.generators.{ ArticleGenerator, CommentGenerator }
import com.real.world.http4s.model.article.{
  ArticleResponse,
  ArticleResponseListWrapper,
  ArticleResponseWrapper,
  CreateArticle,
  CreateArticleWrapper,
  IsFavorited,
  UpdateArticle,
  UpdateArticleWrapper
}
import com.real.world.http4s.model.comment.{ CommentListResponseOutWrapper, CommentResponseWrapper, CreateComment, CreateCommentWrapper }
import com.real.world.http4s.quill.Articles
import org.scalatest.OptionValues
import org.scalatest.flatspec.AsyncFlatSpec

import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.{ Decoder, Encoder }

import cats.effect.IO

import org.http4s.Credentials.Token
import org.http4s._
import org.http4s.circe.{ CirceEntityDecoder, CirceEntityEncoder }
import org.http4s.headers.Authorization
import org.http4s.implicits._

class ArticleRoutesSpec extends AsyncFlatSpec with ServicesAndRepos with CirceEntityDecoder with CirceEntityEncoder with OptionValues {

  // response.bodyAsText.compile.string.unsafeRunSync()

  implicit val CommentEncoder: Encoder[CreateComment]               = deriveEncoder[CreateComment]
  implicit val CommentWrapperEncoder: Encoder[CreateCommentWrapper] = deriveEncoder[CreateCommentWrapper]

  implicit val CreateArticleEncoder: Encoder[CreateArticle] = deriveEncoder[CreateArticle].mapJsonObject(_.filter {
    case (_, v) => !v.isNull // drop null values
  })

  implicit val UpdateArticleEncoder: Encoder[UpdateArticle] =
    deriveEncoder[UpdateArticle].mapJsonObject(_.filter {
      case (_, v) => !v.isNull // drop null values
    })

  implicit val CreateArticleWrapperEncoder: Encoder[CreateArticleWrapper] = deriveEncoder[CreateArticleWrapper]
  implicit val UpdateArticleWrapperEncoder: Encoder[UpdateArticleWrapper] = deriveEncoder[UpdateArticleWrapper]

  implicit val ArticleResponseDecoder: Decoder[ArticleResponse]                       = deriveDecoder[ArticleResponse]
  implicit val ArticleResponseWrapperDecoder: Decoder[ArticleResponseWrapper]         = deriveDecoder[ArticleResponseWrapper]
  implicit val ArticleResponseListWrapperDecoder: Decoder[ArticleResponseListWrapper] = deriveDecoder[ArticleResponseListWrapper]

  private val apiArticles: Uri = uri"/api/articles"

  "App" should "allow user to favorite article" in IOSuit {
    for {
      persistedUser         <- insertUser()
      authorUser            <- insertUser()
      (persistedArticle, _) <- insertArticle(authorUser.id)
      jwt                   <- ctx.jwtAuthenticator.generateJwt(persistedUser.id)
      request = Request[IO](
        method  = Method.POST,
        uri     = apiArticles / persistedArticle.slug.value / "favorite",
        headers = Headers.of(Authorization(Token(TokenAuthScheme, jwt)))
      )
      response               <- ctx.endpoints.run(request)
      _                      <- IO(response.status shouldBe Status.Ok)
      articleResponseWrapper <- response.as[ArticleResponseWrapper]
      _                      <- IO(articleResponseWrapper.article.favorited shouldBe Favorited)
      isFavorited            <- ctx.articleService.isFavoritedByUser(persistedArticle.id, persistedUser.id)
    } yield isFavorited shouldBe IsFavorited.Favorited
  }

  it should "allow user to unfavorite article" in IOSuit {
    for {
      persistedUser         <- insertUser()
      authorUser            <- insertUser()
      (persistedArticle, _) <- insertArticle(authorUser.id)
      _                     <- ctx.articleService.favoriteArticleBySlug(persistedArticle.slug, persistedUser.id)
      jwt                   <- ctx.jwtAuthenticator.generateJwt(persistedUser.id)

      request = Request[IO](
        method  = Method.DELETE,
        uri     = apiArticles / persistedArticle.slug.value / "favorite",
        headers = Headers.of(Authorization(Token(TokenAuthScheme, jwt)))
      )
      response               <- ctx.endpoints.run(request)
      _                      <- IO(response.status shouldBe Status.Ok)
      articleResponseWrapper <- response.as[ArticleResponseWrapper]
      _                      <- IO(articleResponseWrapper.article.favorited shouldBe NotFavorited)
      isFavorited            <- ctx.articleService.isFavoritedByUser(persistedArticle.id, persistedUser.id)
    } yield isFavorited shouldBe IsFavorited.NotFavorited
  }

  it should "allow user to add comment to an article" in IOSuit {
    for {
      persistedUser         <- insertUser()
      authorUser            <- insertUser()
      (persistedArticle, _) <- insertArticle(authorUser.id)
      _                     <- ctx.articleService.favoriteArticleBySlug(persistedArticle.slug, persistedUser.id)
      createComment = CommentGenerator.generateCreateComment
      jwt <- ctx.jwtAuthenticator.generateJwt(persistedUser.id)
      request = Request[IO](
        method  = Method.POST,
        uri     = apiArticles / persistedArticle.slug.value / "comments",
        body    = createComment.toJsonBody,
        headers = Headers.of(Authorization(Token(TokenAuthScheme, jwt)))
      )
      response          <- ctx.endpoints.run(request)
      _                 <- IO(response.status shouldBe Status.Ok)
      _                 <- response.as[CommentResponseWrapper]
      commentsWithUsers <- ctx.commentService.findCommentsWithAuthorByArticleId(persistedArticle.id)
    } yield {
      commentsWithUsers should have size 1
      commentsWithUsers.head._1.body shouldBe createComment.comment.body
      commentsWithUsers.head._2.id shouldBe persistedUser.id
    }
  }

  it should "allow user to create an article" in IOSuit {
    for {
      persistedUser <- insertUser()
      articleRequestIn = ArticleGenerator.generateCreateArticleWrapper
      jwt <- ctx.jwtAuthenticator.generateJwt(persistedUser.id)
      request = Request[IO](
        method  = Method.POST,
        uri     = apiArticles,
        body    = articleRequestIn.toJsonBody,
        headers = Headers.of(Authorization(Token(TokenAuthScheme, jwt)))
      )
      response           <- ctx.endpoints.run(request)
      _                  <- IO(response.status shouldBe Status.Ok)
      articleResponseOut <- response.as[ArticleResponseWrapper]
      // should check before that this returns NotFollowing? Maybe?
      article <- ctx.articleService.findArticleBySlug(articleResponseOut.article.slug)
    } yield articleResponseOut.article.slug shouldBe article.slug
  }

  it should "allow user to delete article" in FailedIOSuit {
    for {
      persistedUser         <- insertUser()
      (persistedArticle, _) <- insertArticle(persistedUser.id)
      jwt                   <- ctx.jwtAuthenticator.generateJwt(persistedUser.id)
      request = Request[IO](
        method  = Method.DELETE,
        uri     = apiArticles / persistedArticle.slug.value,
        headers = Headers.of(Authorization(Token(TokenAuthScheme, jwt)))
      )
      response <- ctx.endpoints.run(request)
      _        <- IO(response.status shouldBe Status.Ok)
      // should check before that this returns NotFollowing? Maybe?
      article <- ctx.articleService.findArticleBySlug(persistedArticle.slug)
    } yield fail("Failed to delete article")
  }(_ shouldBe a[ArticleNotFound])

  it should "allow user to delete comment" in IOSuit {
    for {
      persistedUser         <- insertUser()
      (persistedArticle, _) <- insertArticle(persistedUser.id)
      comment = CommentGenerator.generateComment()
      persistedComment <- ctx.commentService.createComment(comment.body, persistedArticle.slug, persistedUser.id)
      jwt              <- ctx.jwtAuthenticator.generateJwt(persistedUser.id)
      request = Request[IO](
        method  = Method.DELETE,
        uri     = apiArticles / persistedArticle.slug.value / "comments" / persistedComment.id.value.toString,
        headers = Headers.of(Authorization(Token(TokenAuthScheme, jwt)))
      )
      response <- ctx.endpoints.run(request)
      _        <- IO(response.status shouldBe Status.Ok)
      // should check before that this returns NotFollowing? Maybe?
      commentsAndAuthors <- ctx.commentService.findCommentsWithAuthorByArticleId(persistedArticle.id)
    } yield commentsAndAuthors shouldBe empty
  }

  it should "allow user to list comments of an article" in IOSuit {
    for {
      persistedUser         <- insertUser()
      (persistedArticle, _) <- insertArticle(persistedUser.id)
      comment1 = CommentGenerator.generateComment()
      persistedComment1 <- ctx.commentService.createComment(comment1.body, persistedArticle.slug, persistedUser.id)
      comment2 = CommentGenerator.generateComment()
      persistedComment2 <- ctx.commentService.createComment(comment2.body, persistedArticle.slug, persistedUser.id)
      jwt               <- ctx.jwtAuthenticator.generateJwt(persistedUser.id)
      request = Request[IO](
        method  = Method.GET,
        uri     = apiArticles / persistedArticle.slug.value / "comments",
        headers = Headers.of(Authorization(Token(TokenAuthScheme, jwt)))
      )
      response <- ctx.endpoints.run(request)
      _        <- IO(response.status shouldBe Status.Ok)
      // should check before that this returns NotFollowing? Maybe?
      commentListResponseOutWrapper <- response.as[CommentListResponseOutWrapper]
    } yield commentListResponseOutWrapper.comments should have size 2
  }

  it should "allow non-user to list comments of an article" in IOSuit {
    for {
      persistedUser         <- insertUser()
      (persistedArticle, _) <- insertArticle(persistedUser.id)
      comment1 = CommentGenerator.generateComment()
      _ <- ctx.commentService.createComment(comment1.body, persistedArticle.slug, persistedUser.id)
      comment2 = CommentGenerator.generateComment()
      _ <- ctx.commentService.createComment(comment2.body, persistedArticle.slug, persistedUser.id)
      request = Request[IO](
        method = Method.GET,
        uri    = apiArticles / persistedArticle.slug.value / "comments"
      )
      response                      <- ctx.endpoints.run(request)
      _                             <- IO(response.status shouldBe Status.Ok)
      commentListResponseOutWrapper <- response.as[CommentListResponseOutWrapper]
    } yield commentListResponseOutWrapper.comments should have size 2
  }

  it should "allow user to retrieve an article" in IOSuit {
    for {
      persistedUser         <- insertUser()
      (persistedArticle, _) <- insertArticle(persistedUser.id)
      jwt                   <- ctx.jwtAuthenticator.generateJwt(persistedUser.id)
      request = Request[IO](
        method  = Method.GET,
        uri     = apiArticles / persistedArticle.slug.value,
        headers = Headers.of(Authorization(Token(TokenAuthScheme, jwt)))
      )
      response <- ctx.endpoints.run(request)
      _        <- IO(response.status shouldBe Status.Ok)
      // should check before that this returns NotFollowing? Maybe?
      articleResponseOutWrapper <- response.as[ArticleResponseWrapper]
    } yield articleResponseOutWrapper.article.slug shouldBe persistedArticle.slug
  }

  it should "allow anyone to retrieve an article" in IOSuit {
    for {
      persistedUser         <- insertUser()
      (persistedArticle, _) <- insertArticle(persistedUser.id)
      request = Request[IO](method = Method.GET, uri = apiArticles / persistedArticle.slug.value)
      response <- ctx.endpoints.run(request)
      _        <- IO(response.status shouldBe Status.Ok)
      // should check before that this returns NotFollowing? Maybe?
      articleResponseOutWrapper <- response.as[ArticleResponseWrapper]
    } yield {
      articleResponseOutWrapper.article.slug shouldBe persistedArticle.slug
      articleResponseOutWrapper.article.favorited shouldBe NotFavorited
      articleResponseOutWrapper.article.author.following shouldBe NotFollowing
    }
  }

  it should "allow user to update an article" in IOSuit {
    for {
      persistedUser         <- insertUser()
      (persistedArticle, _) <- insertArticle(persistedUser.id)
      jwt                   <- ctx.jwtAuthenticator.generateJwt(persistedUser.id)
      updateArticleRequestInWrapper = ArticleGenerator.generateUpdateArticleWrapper
      request = Request[IO](
        method  = Method.PUT,
        uri     = apiArticles / persistedArticle.slug.value,
        body    = updateArticleRequestInWrapper.toJsonBody,
        headers = Headers.of(Authorization(Token(TokenAuthScheme, jwt)))
      )
      response                  <- ctx.endpoints.run(request)
      _                         <- IO(response.status shouldBe Status.Ok)
      articleResponseOutWrapper <- response.as[ArticleResponseWrapper]
    } yield {
      articleResponseOutWrapper.article.slug should not be persistedArticle.slug
      articleResponseOutWrapper.article.title shouldBe updateArticleRequestInWrapper.article.title.value
      articleResponseOutWrapper.article.description shouldBe updateArticleRequestInWrapper.article.description.value
      articleResponseOutWrapper.article.body shouldBe updateArticleRequestInWrapper.article.body.value
    }
  }
  // --------------------
  it should "allow user to list articles" in IOSuit {
    for {
      persistedUser1    <- insertUser()
      persistedUser2    <- insertUser()
      persistedUser3    <- insertUser()
      persistedArticle1 <- Articles.insertArticle(persistedUser1.id)
      persistedArticle2 <- Articles.insertArticle(persistedUser1.id)
      persistedArticle3 <- Articles.insertArticle(persistedUser2.id)
      persistedArticle4 <- Articles.insertArticle(persistedUser2.id)
      request = Request[IO](method = Method.GET, uri = apiArticles +? ("author", persistedUser1.username.value))
      response                   <- ctx.endpoints.run(request)
      _                          <- IO(response.status shouldBe Status.Ok)
      articleResponseListWrapper <- response.as[ArticleResponseListWrapper]
    } yield articleResponseListWrapper.articles should have size 2
  }

  it should "allow user to retrieve feed" in IOSuit {
    for {
      persistedUser1    <- insertUser()
      persistedUser2    <- insertUser()
      persistedUser3    <- insertUser()
      persistedArticle1 <- Articles.insertArticle(persistedUser1.id)
      persistedArticle2 <- Articles.insertArticle(persistedUser1.id)
      persistedArticle3 <- Articles.insertArticle(persistedUser2.id)
      persistedArticle4 <- Articles.insertArticle(persistedUser2.id)
      persistedArticle5 <- Articles.insertArticle(persistedUser3.id)
      _                 <- ctx.followerService.follow(persistedUser2.id, persistedUser1.id)
      _                 <- ctx.followerService.follow(persistedUser3.id, persistedUser1.id)
      jwt               <- ctx.jwtAuthenticator.generateJwt(persistedUser1.id)
      request = Request[IO](
        method  = Method.GET,
        uri     = apiArticles / "feed",
        headers = Headers.of(Authorization(Token(TokenAuthScheme, jwt)))
      )
      response                   <- ctx.endpoints.run(request)
      _                          <- IO(response.status shouldBe Status.Ok)
      articleResponseListWrapper <- response.as[ArticleResponseListWrapper]
    } yield articleResponseListWrapper.articles should have size 3
  }

//  it should "allow user to update" in SuccessfulIOSuit {
//    val updateUserRequest: UpdateUserRequestInWrapper = UpdateUserRequestInGenerator.generateUpdateUserRequestInWrapper()
//    for {
//      persistedUser <- insertUser()
//      request = AuthedRequest[IO, UserId](
//        persistedUser.id,
//        Request[IO](
//          method = Method.PUT,
//          uri    = uri"/api/user",
//          body   = strBody(updateUserRequest.asJson.spaces2)
//        )
//      )
//      response               <- userHttpRoutes.run(request).attemptT.leftMap(ExceptionWrapper)
//      userResponseOutWrapper <- response.as[UserResponseOutWrapper] |> liftF
//    } yield {
//      val userResponseOut = updateUserRequest.user
//      Some(userResponseOutWrapper.user.email) shouldBe userResponseOut.email
//      Some(userResponseOutWrapper.user.username) shouldBe userResponseOut.username
//      userResponseOutWrapper.user.bio shouldBe userResponseOut.bio
//      userResponseOutWrapper.user.image shouldBe userResponseOut.image
//    }
//  }

}
