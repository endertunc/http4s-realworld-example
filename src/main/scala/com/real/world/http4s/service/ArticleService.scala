package com.real.world.http4s.service

import cats.data.NonEmptyList
import cats.effect.{ Async, Sync }
import cats.implicits._
import com.colisweb.tracing.core.TracingContext
import com.real.world.http4s.AppError.ArticleNotFound
import com.real.world.http4s.model._
import com.real.world.http4s.model.article.{ Article, CreateArticleRequest, IsFavorited, UpdateArticleRequest }
import com.real.world.http4s.model.profile.{ IsFollowing, Profile }
import com.real.world.http4s.model.profile.{ IsFollowing, Profile }
import com.real.world.http4s.model.tag.Tag
import com.real.world.http4s.model.tag.Tag
import com.real.world.http4s.model.tag.Tag.TagName
import com.real.world.http4s.model.{ profile, Pagination }
import com.real.world.http4s.repository.algebra.ArticleRepositoryAlgebra
import com.real.world.http4s.repository.algebra.ArticleRepositoryAlgebra
import eu.timepit.refined.types.numeric.NonNegInt
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger

class ArticleService[F[_]: Async]()(
    implicit L: SelfAwareStructuredLogger[F],
    articlesRepositoryAlgebra: ArticleRepositoryAlgebra[F],
    userService: UserService[F],
    tagService: TagService[F],
    followerService: FollowerService[F]
) {

  def createArticle(createArticle: CreateArticleRequest, userId: UserId)(implicit tracingContext: TracingContext[F]): F[(Article, List[Tag])] =
    for {
      _                <- L.trace(s"Creating article with user: [$userId] and data: [$createArticle]")
      article          <- Article.fromCreateArticleRequest(createArticle, userId).pure[F]
      persistedArticle <- articlesRepositoryAlgebra.createArticle(article)
      tags <- createArticle.tagList.fold(List.empty[Tag].pure[F]) { tagInList =>
        for {
          tags <- tagService.createTags(tagInList)
          _    <- tagService.createArticleTagAssociation(persistedArticle.id, tags)
        } yield tags
      }
      _ <- L.trace("Article is created successfully.")
    } yield (persistedArticle, tags)

  // ToDo update request should be invalid if all field are None in UpdateArticleRequest I guess?
  def updateArticle(updateArticleRequest: UpdateArticleRequest, slug: Slug, userId: UserId)(implicit tracingContext: TracingContext[F]): F[Article] =
    for {
      _       <- L.trace(s"Updating article with user: [$userId], data: [$updateArticleRequest] and slug: [$slug]")
      article <- findArticleBySlug(slug)
      _ <- Sync[F].ifM((article.authorId == userId).pure[F])(
        article.pure[F],
        L.warn(s"User [$userId] attempt to update unauthorized article; id=[${article.id}] slug=[${article.slug}]") *>
        ArticleNotFound(s"Article with slug [$slug] not found").raiseError[F, Article]
      )
      updatedArticle = Article.fromUpdateArticle(article, updateArticleRequest)
      updatedArticleAsOpt    <- articlesRepositoryAlgebra.updateArticle(updatedArticle)
      _                      <- L.warn(s"Article with slug: [$slug] is not found").whenA(updatedArticleAsOpt.isEmpty)
      persistedUpdateArticle <- Sync[F].fromOption(updatedArticleAsOpt, ArticleNotFound(s"Article with slug [$slug] is not found"))
      _                      <- L.trace("Article is updated successfully.")
    } yield persistedUpdateArticle

  def findArticleBySlug(slug: Slug)(implicit tracingContext: TracingContext[F]): F[Article] =
    for {
      _            <- L.trace(s"Trying to find article by slug: [$slug]")
      articleAsOpt <- articlesRepositoryAlgebra.findBySlug(slug)
      _            <- L.warn(s"Article with slug: [$slug] is not found").whenA(articleAsOpt.isEmpty)
      article      <- Sync[F].fromOption(articleAsOpt, ArticleNotFound(s"Article with slug [$slug] is not found"))
      // Another way to doing the same thing // ToDo maybe remove this bullshit
//      article <- articleAsOpt.liftTo[Either[AppError, *]](ArticleNotFound(s"Article with the slug [$slug] is not found")).liftTo[F]
      _ <- L.trace(s"Found article with slug [$slug]")
    } yield article

  def deleteArticleBySlugAndUserId(slug: Slug, userId: UserId)(implicit tracingContext: TracingContext[F]): F[Unit] =
    for {
      _ <- L.trace(s"User [$userId] is trying to delete article with slug [$slug]")
      _ <- articlesRepositoryAlgebra.deleteBySlugAndUserId(slug, userId)
      _ <- L.trace(s"User [$userId] deleted article with slug [$slug] successfully")
    } yield ()

  def favoriteArticleBySlug(slug: Slug, userId: UserId)(implicit tracingContext: TracingContext[F]): F[Unit] =
    for {
      _       <- L.trace(s"User [$userId] is trying to favorite article with slug [$slug]")
      article <- findArticleBySlug(slug)
      _       <- articlesRepositoryAlgebra.favorite(article.id, userId)
      _       <- L.trace(s"User [$userId] successfully favorited article with slug [$slug]")
    } yield ()

  def unfavoriteArticleBySlug(slug: Slug, userId: UserId)(implicit tracingContext: TracingContext[F]): F[Unit] =
    for {
      _       <- L.trace(s"User [$userId] is trying to unfavorite article with slug [$slug]")
      article <- findArticleBySlug(slug)
      _       <- articlesRepositoryAlgebra.unfavorite(article.id, userId)
      _       <- L.trace(s"User [$userId] successfully unfavorited article with slug [$slug]")
    } yield ()

  def isFavoritedByUser(articleId: ArticleId, userId: UserId)(implicit tracingContext: TracingContext[F]): F[IsFavorited] =
    for {
      _           <- L.trace(s"Checking if user [$userId] is followed article [$articleId]")
      isFavorited <- articlesRepositoryAlgebra.isFavoritedByUser(articleId, userId).map(IsFavorited(_))
    } yield isFavorited

  def findFavoritedCount(articleId: ArticleId)(implicit tracingContext: TracingContext[F]): F[FavoritesCount] =
    articlesRepositoryAlgebra
      .findFavoritedRecordsByArticleId(articleId)
      .map(favoritedRecordsList => FavoritesCount(NonNegInt.unsafeFrom(favoritedRecordsList.size)))

  def fetchArticle(slug: Slug, optionalUserId: Option[UserId])(
      implicit tracingContext: TracingContext[F]
  ): F[(Article, Profile, List[Tag], IsFavorited, FavoritesCount)] =
    for {
      article        <- findArticleBySlug(slug)
      tags           <- tagService.findTagsByArticleId(article.id)
      author         <- userService.findUserById(article.authorId)
      isFollowing    <- optionalUserId.map(followerService.isFollowing(article.authorId, _)).getOrElse(IsFollowing.NotFollowing.pure[F])
      isFavorited    <- optionalUserId.map(isFavoritedByUser(article.id, _)).getOrElse(IsFavorited.NotFavorited.pure[F])
      favoritedCount <- findFavoritedCount(article.id)
    } yield (article, profile.Profile(author, isFollowing), tags, isFavorited, favoritedCount)

  def findAll(
      maybeAuthorUsername: Option[Username],
      maybeFavoritedAuthorUsername: Option[Username],
      maybeTagName: Option[TagName],
      maybeUserId: Option[UserId],
      pagination: Pagination
  )(implicit tracingContext: TracingContext[F]): F[List[(Article, Profile, List[Tag], IsFavorited, FavoritesCount)]] =
    articlesRepositoryAlgebra
      .listArticles(maybeAuthorUsername, maybeFavoritedAuthorUsername, maybeTagName, pagination)
      .flatMap(toArticleWithMetadata(_, maybeUserId))

  def feed(userId: UserId, pagination: Pagination)(
      implicit tracingContext: TracingContext[F]
  ): F[List[(Article, Profile, List[Tag], IsFavorited, FavoritesCount)]] =
    for {
      _    <- L.trace(s"Retrieving feed for user [$userId]")
      feed <- articlesRepositoryAlgebra.listArticlesByFavoritedUsers(userId, pagination).flatMap(toArticleWithMetadata(_, Some(userId)))
      _    <- L.trace(s"Found [${feed.length}] of articles on user [$userId]s feed")
    } yield feed

  def findFavoritedsByUserId(articleIds: NonEmptyList[ArticleId], userId: Option[UserId])(
      implicit tracingContext: TracingContext[F]
  ): F[Set[ArticleId]] =
    userId.map(userId => articlesRepositoryAlgebra.findFavoritedsByUserId(articleIds, userId)).getOrElse(Set.empty[ArticleId].pure[F])

  private def toArticleWithMetadata(
      articles: List[Article],
      userId: Option[UserId]
  )(implicit tracingContext: TracingContext[F]): F[List[(Article, Profile, List[Tag], IsFavorited, FavoritesCount)]] =
    NonEmptyList.fromList(articles) match {
      case Some(articles) =>
        retrieveMetadata(userId, articles).map(_.toList)
      case None => List.empty[(Article, Profile, List[Tag], IsFavorited, FavoritesCount)].pure[F]
    }

  private def retrieveMetadata(
      maybeUserId: Option[UserId],
      articles: NonEmptyList[Article]
  )(implicit tracingContext: TracingContext[F]): F[NonEmptyList[(Article, Profile, List[Tag], IsFavorited, FavoritesCount)]] = {
    val nelArticleIds: NonEmptyList[ArticleId] = articles.map(_.id)
    for {
      profiles              <- userService.findProfilesByUserId(articles.map(_.authorId), maybeUserId)
      tags                  <- tagService.findTagsByArticleIds(nelArticleIds)
      isFavoriteds          <- findFavoritedsByUserId(nelArticleIds, maybeUserId)
      articleFavoriteCounts <- articlesRepositoryAlgebra.findArticlesFavoriteCount(nelArticleIds)
    } yield {
      articles.map { article =>
        val profile        = profiles(article.authorId) // I don't use transactions and this can throw
        val articleTags    = tags.getOrElse(article.id, List.empty[Tag])
        val isFavorited    = IsFavorited(isFavoriteds.contains(article.id))
        val favoritedCount = articleFavoriteCounts.getOrElse(article.id, FavoritesCount.zeroFavorite)
        (article, profile, articleTags, isFavorited, favoritedCount)
      }
    }
  }
}

object ArticleService {

  def apply[F[_]: Async: SelfAwareStructuredLogger]()(
      implicit articlesRepositoryAlgebra: ArticleRepositoryAlgebra[F],
      userService: UserService[F],
      tagService: TagService[F],
      followerService: FollowerService[F]
  ): ArticleService[F] =
    new ArticleService()
}
