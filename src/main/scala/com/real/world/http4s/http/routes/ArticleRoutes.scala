package com.real.world.http4s.http.routes

import scala.util.Try
import org.http4s.{ AuthedRoutes, ParseFailure, QueryParamDecoder }
import cats.effect.Async
import cats.implicits._
import com.colisweb.tracing.core.TracingContext
import com.real.world.http4s.http.middleware.AuthedTracedRoutes.{ using, AuthedTraceContext, AuthedTracedRoutes }
import com.real.world.http4s.http.BaseHttp4s
import com.real.world.http4s.http.middleware.AuthedTracedRoutes
import com.real.world.http4s.model._
import com.real.world.http4s.model.article._
import com.real.world.http4s.model.comment.Comment.CommentId
import com.real.world.http4s.model.comment._
import com.real.world.http4s.model.profile.IsFollowing.NotFollowing
import com.real.world.http4s.model.profile._
import com.real.world.http4s.model.tag.Tag.TagName
import com.real.world.http4s.model.{ article, Pagination }
import com.real.world.http4s.service._
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.{ NonNegative, Positive }
import eu.timepit.refined.refineV
import io.chrisdavenport.log4cats.Logger

class ArticleRoutes[F[_]: Async: Logger]()(
    implicit articleService: ArticleService[F],
    commentService: CommentService[F],
    userService: UserService[F],
    tagService: TagService[F],
    profileService: ProfileService[F]
) extends BaseHttp4s[F] {

  implicit val userIdQueryParamDecoder: QueryParamDecoder[UserId] =
    QueryParamDecoder[Int].emap(userId => refineV[NonNegative](userId).bimap(err => new ParseFailure(err, err), UserId.apply))

  implicit val usernameQueryParamDecoder: QueryParamDecoder[Username] =
    QueryParamDecoder[String].emap(userId => refineV[NonEmpty](userId).bimap(err => new ParseFailure(err, err), Username.apply))

  object OptionalTagQueryParamMatcher extends OptionalQueryParamDecoderMatcher[String]("tag")
  object OptionalAuthorQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Username]("author")
  object OptionalFavoritedQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Username]("favorited")
  object OptionalLimitQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Long]("limit")
  object OptionalOffsetQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Long]("offset")

  // I was too lazy to make sure that limit and offsets are valid...
  private val defaultLimit = 20
  private val defaultPage  = 0

  object SlugVar {
    def unapply(slugStr: String): Option[Slug] = refineV[NonEmpty](slugStr).toOption.map(Slug.apply)
  }

  object CommentIdVar {
    // WTFF
    def unapply(commentIdStr: String): Option[CommentId] =
      Try(commentIdStr.toInt).map(refineV[Positive](_).toOption.map(CommentId.apply)).toOption.flatten
  }

  val optionallySecuredRoutes: AuthedRoutes[Option[UserId], F] =
    AuthedRoutes.of {
      case GET -> Root / "feed"
          :? OptionalLimitQueryParamMatcher(limit)
          :? OptionalOffsetQueryParamMatcher(offset) as Some(userId) =>
        val pagination = Pagination(limit.getOrElse(defaultLimit), offset.getOrElse(defaultPage))
        for {
          articlesWithMetadata <- articleService.feed(userId, pagination)
          response             <- Ok(ArticleResponseListWrapper(articlesWithMetadata))
        } yield response

      case GET -> Root / SlugVar(slug) as optionalUserId =>
        articleService.fetchArticle(slug, optionalUserId).map(ArticleResponseWrapper.create.tupled(_)) >>= (Ok(_)) // fancy flatMap :)

      case GET -> Root / SlugVar(slug) / "comments" as optionalUserId =>
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
          :? OptionalOffsetQueryParamMatcher(offset) as maybeUserId =>
        val pagination = Pagination(limit.getOrElse(defaultLimit), offset.getOrElse(defaultPage))
        for {
          articlesWithMetadata <- articleService.findAll(
            maybeAuthorUsername          = authorUsername,
            maybeFavoritedAuthorUsername = favorited,
            maybeTagName                 = optionalTag.map(TagName),
            maybeUserId                  = maybeUserId,
            pagination                   = pagination
          )
          response <- Ok(article.ArticleResponseListWrapper(articlesWithMetadata))
        } yield response
    }

  val securedRoutes: AuthedTracedRoutes[F] =
    AuthedTracedRoutes.of[F] {
      // Post
      case (req @ POST -> Root) using AuthedTraceContext(userId, implicit0(context: TracingContext[F])) =>
        req.decode[CreateArticleWrapper] { createArticleRequestInWrapper =>
          val createArticleRequestIn = createArticleRequestInWrapper.article
          for {
            (article, tags) <- articleService.createArticle(createArticleRequestIn, userId)
            author          <- userService.findUserById(article.authorId)
            response <- Ok(
              ArticleResponseWrapper(
                article        = article,
                author         = Profile(author, NotFollowing),
                tags           = tags,
                favorited      = IsFavorited.NotFavorited,
                favoritesCount = zeroFavorites
              )
            )
          } yield response
        }

      case (req @ POST -> Root / SlugVar(slug) / "comments") using AuthedTraceContext(userId, implicit0(context: TracingContext[F])) =>
        req.decode[CreateCommentWrapper] { commentInWrapper =>
          val commentIn = commentInWrapper.comment
          for {
            comment  <- commentService.createComment(commentIn.body, slug, userId)
            profile  <- profileService.findProfileByUserId(comment.authorId, userId)
            response <- Ok(CommentResponseWrapper(comment = comment, author = profile))
          } yield response
        }

      case (POST -> Root / SlugVar(slug) / "favorite") using AuthedTraceContext(userId, implicit0(context: TracingContext[F])) =>
        for {
          _                      <- articleService.favoriteArticleBySlug(slug, userId)
          articleResponseWrapper <- articleService.fetchArticle(slug, Some(userId)).map(ArticleResponseWrapper.create.tupled(_))
          response               <- Ok(articleResponseWrapper)
        } yield response

      // Put
      case (req @ PUT -> Root / SlugVar(slug)) using AuthedTraceContext(userId, implicit0(context: TracingContext[F])) =>
        req.decode[UpdateArticleWrapper] { updateArticleRequestInWrapper =>
          val updateArticleRequestIn = updateArticleRequestInWrapper.article
          for {
            updatedArticle <- articleService.updateArticle(updateArticleRequestIn, slug, userId)
            tags           <- tagService.findTagsByArticleId(updatedArticle.id)
            author         <- userService.findUserById(updatedArticle.authorId)
            favoritedCount <- articleService.findFavoritedCount(updatedArticle.id)
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
      case (DELETE -> Root / SlugVar(slug)) using AuthedTraceContext(userId, implicit0(context: TracingContext[F])) =>
        articleService.deleteArticleBySlugAndUserId(slug, userId) *> Ok()

      // Ideally we could check if given commentId belongs to the given article, but I don't see any reason.
      case (DELETE -> Root / SlugVar(slug @ _) / "comments" / CommentIdVar(commentId)) using AuthedTraceContext(
            userId,
            implicit0(context: TracingContext[F])
          ) =>
        commentService.deleteByCommentIdAndAuthorId(commentId, userId) *> Ok()

      case (DELETE -> Root / SlugVar(slug) / "favorite") using AuthedTraceContext(userId, implicit0(context: TracingContext[F])) =>
        for {
          _                      <- articleService.unfavoriteArticleBySlug(slug, userId)
          articleResponseWrapper <- articleService.fetchArticle(slug, Some(userId)).map(ArticleResponseWrapper.create.tupled(_))
          response               <- Ok(articleResponseWrapper)
        } yield response

    }

}

object ArticleRoutes {
  def apply[F[_]: Async: Logger: ArticleService: CommentService: UserService: TagService: ProfileService](): ArticleRoutes[F] =
    new ArticleRoutes[F]()
}
