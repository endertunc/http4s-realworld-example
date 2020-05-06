package com.real.world.http4s.http.routes

import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import org.http4s.{ ParseFailure, QueryParamDecoder }

import cats.effect.Async
import cats.implicits._

import com.colisweb.tracing.core.TracingContext

import com.real.world.http4s.http.Http4sAndCirceSupport
import com.real.world.http4s.http.middleware.TracedContextRoutes
import com.real.world.http4s.http.middleware.TracedContextRoutes.{ using, TracedContext, TracedContextRoutes }
import com.real.world.http4s.http.routes.ArticleRoutes.{ OptionalLimitQueryParamMatcher, OptionalOffsetQueryParamMatcher }
import com.real.world.http4s.model._
import com.real.world.http4s.model.article.{ UpdateArticleInputWrapper, CreateArticleInputWrapper, _ }
import com.real.world.http4s.model.comment.Comment.CommentId
import com.real.world.http4s.model.comment.{ CreateCommentInputWrapper, _ }
import com.real.world.http4s.model.profile.IsFollowing.NotFollowing
import com.real.world.http4s.model.profile._
import com.real.world.http4s.model.tag.Tag.TagName
import com.real.world.http4s.model.{ article, Pagination }
import com.real.world.http4s.service._

import io.chrisdavenport.log4cats.SelfAwareStructuredLogger

class ArticleRoutes[F[_]: Async]()(
    implicit L: SelfAwareStructuredLogger[F],
    articleService: ArticleService[F],
    commentService: CommentService[F],
    userService: UserService[F],
    tagService: TagService[F],
    profileService: ProfileService[F]
) extends Http4sAndCirceSupport[F]
    with ArticleInputValidator
    with CommentInputValidator {

  import ArticleRoutes._

  // I was too lazy to make sure that limit and offsets are valid...
  private val defaultLimit = 20
  private val defaultPage  = 0

  object SlugVar {
    def unapply(slugStr: String): Option[Slug] = Slug.from(slugStr).toOption
  }

  object CommentIdVar {
    def unapply(commentIdStr: String): Option[CommentId] = CommentId.from(commentIdStr.toInt).toOption
  }

  val optionallySecuredRoutes: TracedContextRoutes[F, Option[UserId]] =
    TracedContextRoutes.of[F, Option[UserId]] {
      case GET -> Root / "feed"
          :? OptionalLimitQueryParamMatcher(limit)
          :? OptionalOffsetQueryParamMatcher(offset) as TracedContext(Some(userId), implicit0(tc: TracingContext[F])) =>
        val pagination = Pagination(limit.getOrElse(defaultLimit), offset.getOrElse(defaultPage))
        for {
          articlesWithMetadata <- articleService.feed(userId, pagination)
          response             <- Ok(ArticleResponseListWrapper(articlesWithMetadata))
        } yield response

      case GET -> Root / SlugVar(slug) as TracedContext(optionalUserId, implicit0(tc: TracingContext[F])) =>
        articleService.fetchArticle(slug, optionalUserId).map(ArticleResponseWrapper.create.tupled(_)) >>= (Ok(_)) // fancy flatMap :)

      case GET -> Root / SlugVar(slug) / "comments" as TracedContext(optionalUserId, implicit0(tc: TracingContext[F])) =>
        for {
          article             <- articleService.findArticleBySlug(slug)
          commentsWithProfile <- commentService.findCommentsWithAuthorProfile(article.id, optionalUserId)
          respone <- Ok(
            // Move this to apply method
            CommentListResponseOutWrapper(
              comments = commentsWithProfile.map {
                case (comment, profile) => CommentResponse(comment, profile)
              }
            )
          )
        } yield respone

      // I was too lazy to make sure limit and offsets are valid...
      case GET -> Root
          :? OptionalTagQueryParamMatcher(optionalTag)
          :? OptionalAuthorQueryParamMatcher(authorUsername)
          :? OptionalFavoritedQueryParamMatcher(favorited)
          :? OptionalLimitQueryParamMatcher(limit)
          :? OptionalOffsetQueryParamMatcher(offset)
          as TracedContext(maybeUserId, implicit0(context: TracingContext[F])) =>
        val pagination = Pagination(limit.getOrElse(defaultLimit), offset.getOrElse(defaultPage))
        for {
          articlesWithMetadata <- articleService.findAll(
            maybeAuthorUsername          = authorUsername,
            maybeFavoritedAuthorUsername = favorited,
            maybeTagName                 = optionalTag,
            maybeUserId                  = maybeUserId,
            pagination                   = pagination
          )
          response <- Ok(article.ArticleResponseListWrapper(articlesWithMetadata))
        } yield response
    }

  val securedRoutes: TracedContextRoutes[F, UserId] =
    TracedContextRoutes.of[F, UserId] {
      // Post
      case (req @ POST -> Root) using TracedContext(userId, implicit0(context: TracingContext[F])) =>
        req.decode[CreateArticleInputWrapper] { createArticleInputWrapper =>
          val createArticleInput = createArticleInputWrapper.article
          for {
            createArticleRequest <- validateCreateArticleInput(createArticleInput)
            (article, tags)      <- articleService.createArticle(createArticleRequest, userId)
            author               <- userService.findUserById(article.authorId)
            response <- Ok(
              ArticleResponseWrapper(
                article        = article,
                author         = Profile(author, NotFollowing),
                tags           = tags,
                favorited      = IsFavorited.NotFavorited,
                favoritesCount = FavoritesCount.zeroFavorite
              )
            )
          } yield response
        }

      case (req @ POST -> Root / SlugVar(slug) / "comments") using TracedContext(userId, implicit0(context: TracingContext[F])) =>
        req.decode[CreateCommentInputWrapper] { createCommentInputWrapper =>
          val createCommentInput = createCommentInputWrapper.comment
          for {
            createCommentRequest <- validateCreateCommentInput(createCommentInput)
            comment              <- commentService.createComment(createCommentRequest.body, slug, userId)
            profile              <- profileService.findProfileByUserId(comment.authorId, userId)
            response             <- Ok(CommentResponseWrapper(comment = comment, author = profile))
          } yield response
        }

      case (POST -> Root / SlugVar(slug) / "favorite") using TracedContext(userId, implicit0(context: TracingContext[F])) =>
        for {
          _                      <- articleService.favoriteArticleBySlug(slug, userId)
          articleResponseWrapper <- articleService.fetchArticle(slug, Some(userId)).map(ArticleResponseWrapper.create.tupled(_))
          response               <- Ok(articleResponseWrapper)
        } yield response

      // Put
      case (req @ PUT -> Root / SlugVar(slug)) using TracedContext(userId, implicit0(context: TracingContext[F])) =>
        req.decode[UpdateArticleInputWrapper] { updateArticleInputWrapper =>
          val updateArticleInput = updateArticleInputWrapper.article
          for {
            updateArticleRequest <- validateUpdateArticleInput(updateArticleInput)
            updatedArticle       <- articleService.updateArticle(updateArticleRequest, slug, userId)
            tags                 <- tagService.findTagsByArticleId(updatedArticle.id)
            author               <- userService.findUserById(updatedArticle.authorId)
            favoritedCount       <- articleService.findFavoritedCount(updatedArticle.id)
            response <- Ok(
              ArticleResponseWrapper(
                article        = updatedArticle,
                author         = Profile(author, IsFollowing(false)),
                tags           = tags,
                favorited      = IsFavorited.NotFavorited,
                favoritesCount = favoritedCount
              )
            )
          } yield response
        }

      // Delete
      case (DELETE -> Root / SlugVar(slug)) using TracedContext(userId, implicit0(context: TracingContext[F])) =>
        articleService.deleteArticleBySlugAndUserId(slug, userId) *> Ok()

      // Ideally we could check if given commentId belongs to the given article, but I don't see any reason.
      case (DELETE -> Root / SlugVar(slug @ _) / "comments" / CommentIdVar(commentId)) using TracedContext(
            userId,
            implicit0(context: TracingContext[F])
          ) =>
        commentService.deleteByCommentIdAndAuthorId(commentId, userId) *> Ok()

      case (DELETE -> Root / SlugVar(slug) / "favorite") using TracedContext(userId, implicit0(context: TracingContext[F])) =>
        for {
          _                      <- articleService.unfavoriteArticleBySlug(slug, userId)
          articleResponseWrapper <- articleService.fetchArticle(slug, Some(userId)).map(ArticleResponseWrapper.create.tupled(_))
          response               <- Ok(articleResponseWrapper)
        } yield response

    }

}

object ArticleRoutes {

  // ToDo fix those err message in ParseFailure
  implicit val userIdQueryParamDecoder: QueryParamDecoder[UserId] =
    QueryParamDecoder[Int].emap(userId => UserId.from(userId).leftMap(err => new ParseFailure(err, err)))

  implicit val usernameQueryParamDecoder: QueryParamDecoder[Username] =
    QueryParamDecoder[String].emap(userName => Username.from(userName).leftMap(err => new ParseFailure(err, err)))

  implicit val tagNameQueryParamDecoder: QueryParamDecoder[TagName] =
    QueryParamDecoder[String].emap(tag => TagName.from(tag).leftMap(err => new ParseFailure(err, err)))

  object OptionalTagQueryParamMatcher extends OptionalQueryParamDecoderMatcher[TagName]("tag")
  object OptionalAuthorQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Username]("author")
  object OptionalFavoritedQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Username]("favorited")
  object OptionalLimitQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Long]("limit")
  object OptionalOffsetQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Long]("offset")

  def apply[F[_]: Async: SelfAwareStructuredLogger: ArticleService: CommentService: UserService: TagService: ProfileService](): ArticleRoutes[F] =
    new ArticleRoutes[F]()
}
