package com.real.world.http4s.http.routes

import com.real.world.http4s.http.BaseHttp4s
import com.real.world.http4s.model.article.Article.{ FavoritesCount, Slug }
import com.real.world.http4s.model.article._
import com.real.world.http4s.model.comment.Comment.CommentId
import com.real.world.http4s.model.comment.{ CommentListResponseOutWrapper, CommentResponse, CommentResponseWrapper, CreateCommentWrapper }
import com.real.world.http4s.model.profile.IsFollowing.NotFollowing
import com.real.world.http4s.model.profile.{ IsFollowing, Profile }
import com.real.world.http4s.model.tag.Tag.TagName
import com.real.world.http4s.model.user.User.{ UserId, Username }
import com.real.world.http4s.service._
import com.real.world.http4s.http.BaseHttp4s
import com.real.world.http4s.model.{ article, Pagination }
import com.real.world.http4s.model.article.{
  ArticleResponseListWrapper,
  ArticleResponseWrapper,
  CreateArticleWrapper,
  IsFavorited,
  UpdateArticleWrapper
}
import com.real.world.http4s.model.comment.{ CommentListResponseOutWrapper, CommentResponse, CommentResponseWrapper, CreateCommentWrapper }
import com.real.world.http4s.model.profile.{ IsFollowing, Profile }
import com.real.world.http4s.service.{ ArticleService, CommentService, FollowerService, ProfileService, TagService, UserService }

import cats.effect.Async
import cats.implicits._

import org.http4s.AuthedRoutes

import io.chrisdavenport.log4cats.Logger

class ArticleRoutes[F[_]: Async: Logger]()(
    implicit articleService: ArticleService[F],
    commentService: CommentService[F],
    userService: UserService[F],
    followerService: FollowerService[F],
    tagService: TagService[F],
    profileService: ProfileService[F]
) extends BaseHttp4s {

  object OptionalTagQueryParamMatcher extends OptionalQueryParamDecoderMatcher[String]("tag")
  object OptionalAuthorQueryParamMatcher extends OptionalQueryParamDecoderMatcher[String]("author")
  object OptionalFavoritedQueryParamMatcher extends OptionalQueryParamDecoderMatcher[String]("favorited")
  object OptionalLimitQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Long]("limit")
  object OptionalOffsetQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Long]("offset")

  // I was too lazy to make sure that limit and offsets are valid...
  private val defaultLimit = 20
  private val defaultPage  = 0

  val optionallySecuredRoutes: AuthedRoutes[Option[UserId], F] =
    AuthedRoutes.of {
      case GET -> Root / "feed"
          :? OptionalLimitQueryParamMatcher(limit)
          :? OptionalOffsetQueryParamMatcher(offset) as Some(userId) =>
        val pagination = Pagination(limit.getOrElse(defaultLimit), offset.getOrElse(defaultPage))
        (for {
          articlesWithMetadata <- articleService.feed(userId, pagination)
        } yield ArticleResponseListWrapper(articlesWithMetadata)).toResponse

      case GET -> Root / slug as optionalUserId =>
        articleService.fetchArticle(Slug(slug), optionalUserId).map(ArticleResponseWrapper.create.tupled(_)).toResponse

      case GET -> Root / slug / "comments" as optionalUserId =>
        (for {
          article             <- articleService.findArticleBySlug(Slug(slug))
          commentsWithProfile <- commentService.findCommentsWithAuthorProfile(article.id, optionalUserId)
        } yield CommentListResponseOutWrapper {
          commentsWithProfile.map {
            case (comment, profile) => CommentResponse(comment, profile)
          }
        }).toResponse

      // I was too lazy to make sure limit and offsets are valid...
      case GET -> Root
          :? OptionalTagQueryParamMatcher(optionalTag)
          :? OptionalAuthorQueryParamMatcher(authorUsername)
          :? OptionalFavoritedQueryParamMatcher(favorited)
          :? OptionalLimitQueryParamMatcher(limit)
          :? OptionalOffsetQueryParamMatcher(offset) as maybeUserId =>
        val pagination = Pagination(limit.getOrElse(defaultLimit), offset.getOrElse(defaultPage))
        (for {
          articlesWithMetadata <- articleService.findAll(
            maybeAuthorUsername          = authorUsername.map(Username),
            maybeFavoritedAuthorUsername = favorited.map(Username),
            maybeTagName                 = optionalTag.map(TagName),
            maybeUserId                  = maybeUserId,
            pagination                   = pagination
          )
        } yield article.ArticleResponseListWrapper(articlesWithMetadata)).toResponse
    }

  val securedRoutes: AuthedRoutes[UserId, F] =
    AuthedRoutes.of {

      // Post
      case req @ POST -> Root as userId =>
        req.req.decode[CreateArticleWrapper] { createArticleRequestInWrapper =>
          val createArticleRequestIn = createArticleRequestInWrapper.article
          (for {
            (article, tags) <- articleService.createArticle(createArticleRequestIn, userId)
            author          <- userService.findUserById(article.authorId)
          } yield ArticleResponseWrapper(
            article        = article,
            author         = Profile(author, NotFollowing),
            tags           = tags,
            favorited      = IsFavorited.NotFavorited,
            favoritesCount = FavoritesCount(0)
          )).toResponse
        }

      case req @ POST -> Root / slug / "comments" as userId =>
        req.req.decode[CreateCommentWrapper] { commentInWrapper =>
          val commentIn = commentInWrapper.comment
          (for {
            comment <- commentService.createComment(commentIn.body, Slug(slug), userId)
            profile <- profileService.findProfileByUserId(comment.authorId, userId)
          } yield CommentResponseWrapper(comment = comment, author = profile)).toResponse
        }

      case POST -> Root / slug / "favorite" as userId =>
        (for {
          _                      <- articleService.favoriteArticleBySlug(Slug(slug), userId)
          articleResponseWrapper <- articleService.fetchArticle(Slug(slug), Some(userId)).map(ArticleResponseWrapper.create.tupled(_))
        } yield articleResponseWrapper).toResponse

      // Put
      case req @ PUT -> Root / slug as userId =>
        req.req.decode[UpdateArticleWrapper] { updateArticleRequestInWrapper =>
          val updateArticleRequestIn = updateArticleRequestInWrapper.article
          (for {
            updatedArticle <- articleService.updateArticle(updateArticleRequestIn, Slug(slug), userId)
            tags           <- tagService.findTagsByArticleId(updatedArticle.id)
            author         <- userService.findUserById(updatedArticle.authorId)
            favoritedCount <- articleService.findFavoritedCount(updatedArticle.id)
          } yield article.ArticleResponseWrapper(
            article        = updatedArticle,
            author         = Profile(author, IsFollowing(false)),
            tags           = tags,
            favorited      = IsFavorited.NotFavorited,
            favoritesCount = favoritedCount
          )).toResponse
        }

      // Delete
      case DELETE -> Root / slug as userId => articleService.deleteArticleBySlugAndUserId(Slug(slug), userId).toResponse

      // Ideally we could check if given commentId belongs to the given article, but I don't see any reason.
      case DELETE -> Root / slug / "comments" / IntVar(id) as userId =>
        commentService.deleteByCommentIdAndAuthorId(CommentId(id), userId).toResponse

      case DELETE -> Root / slug / "favorite" as implicit0(userId: UserId) =>
        val result = for {
          _                      <- articleService.unfavoriteArticleBySlug(Slug(slug), userId)
          articleResponseWrapper <- articleService.fetchArticle(Slug(slug), Some(userId)).map(ArticleResponseWrapper.create.tupled(_))
        } yield articleResponseWrapper
        result.toResponse
    }

}

object ArticleRoutes {
  def apply[F[_]: Async: Logger: ArticleService: CommentService: UserService: FollowerService: TagService: ProfileService](): ArticleRoutes[F] =
    new ArticleRoutes[F]()
}
