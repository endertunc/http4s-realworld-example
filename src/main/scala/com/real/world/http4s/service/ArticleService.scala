package com.real.world.http4s.service

import com.real.world.http4s.AppError
import com.real.world.http4s.AppError.ArticleNotFound
import com.real.world.http4s.model.{ ArticleValidators, Pagination }
import com.real.world.http4s.model.article.Article.{ ArticleId, FavoritesCount, Slug }
import com.real.world.http4s.model.article.{ Article, CreateArticle, IsFavorited, UpdateArticle }
import com.real.world.http4s.model.profile.{ IsFollowing, Profile }
import com.real.world.http4s.model.tag.Tag
import com.real.world.http4s.model.tag.Tag.TagName
import com.real.world.http4s.model.user.User.{ UserId, Username }
import com.real.world.http4s.repository.algebra.ArticleRepositoryAlgebra
import com.real.world.http4s.model.article.{ Article, CreateArticle, IsFavorited, UpdateArticle }
import com.real.world.http4s.model.{ profile, ArticleValidators, Pagination }
import com.real.world.http4s.model.profile.{ IsFollowing, Profile }
import com.real.world.http4s.model.tag.Tag
import com.real.world.http4s.repository.algebra.ArticleRepositoryAlgebra
import io.chrisdavenport.log4cats.Logger

import cats.data.NonEmptyList
import cats.effect.{ Async, Sync }
import cats.implicits._

class ArticleService[F[_]: Async: Logger]()(
    implicit articlesRepositoryAlgebra: ArticleRepositoryAlgebra[F],
    userService: UserService[F],
    tagService: TagService[F],
    followerService: FollowerService[F]
) extends ArticleValidators {

  def createArticle(
      createArticle: CreateArticle,
      userId: UserId
  ): F[(Article, List[Tag])] =
    for {
      _                <- Logger[F].trace(s"Creating article with user: [$userId] and data: [$createArticle]")
      _                <- validateCreateArticle(createArticle)
      article          <- createArticle.toArticle(userId).pure[F]
      persistedArticle <- articlesRepositoryAlgebra.createArticle(article)
      tags <- createArticle.tagList.fold(List.empty[Tag].pure[F]) { tagInList =>
        for {
          tags <- tagService.createTags(tagInList)
          _    <- tagService.createArticleTagAssociation(persistedArticle.id, tags)
        } yield tags
      }
      _ <- Logger[F].trace("Article is created successfully.")
    } yield (persistedArticle, tags)

  def updateArticle(articleUpdateIn: UpdateArticle, slug: Slug, userId: UserId): F[Article] =
    for {
      _       <- Logger[F].trace(s"Updating article with user: [$userId], data: [$articleUpdateIn] and slug: [$slug]")
      article <- findArticleBySlug(slug)
      _ <- Sync[F].ifM((article.authorId == userId).pure[F])(
        article.pure[F],
        Logger[F].warn(s"User [$userId] attempt to update unauthorized article; id=[${article.id}] slug=[${article.slug}]") *>
        ArticleNotFound(s"Article with slug [$slug] not found").raiseError[F, Article]
      )
      updatedArticle = articleUpdateIn.merge(article)
      updatedArticleAsOpt    <- articlesRepositoryAlgebra.updateArticle(updatedArticle)
      _                      <- Logger[F].warn(s"Article with slug: [$slug] is not found").whenA(updatedArticleAsOpt.isEmpty)
      persistedUpdateArticle <- Sync[F].fromOption(updatedArticleAsOpt, ArticleNotFound(s"Article with slug [$slug] is not found"))
      _                      <- Logger[F].trace("Article is updated successfully.")
    } yield persistedUpdateArticle

  import mouse.all._

  def findArticleBySlug(slug: Slug): F[Article] =
    for {
      _            <- Logger[F].trace(s"Trying to find article by slug: [$slug]")
      articleAsOpt <- articlesRepositoryAlgebra.findBySlug(slug)
      _            <- Logger[F].warn(s"Article with slug: [$slug] is not found").whenA(articleAsOpt.isEmpty)
      article      <- Sync[F].fromOption(articleAsOpt, ArticleNotFound(s"Article with slug [$slug] is not found"))
      // Another way to doing the same thing // ToDo maybe remove this bullshit
//      article <- articleAsOpt.liftTo[Either[AppError, *]](ArticleNotFound(s"Article with the slug [$slug] is not found")).liftTo[F]
      _ <- Logger[F].trace(s"Found article with slug [$slug]")
    } yield article

  def deleteArticleBySlugAndUserId(slug: Slug, userId: UserId): F[Unit] =
    for {
      _ <- Logger[F].trace(s"User [$userId] is trying to delete article with slug [$slug]")
      _ <- articlesRepositoryAlgebra.deleteBySlugAndUserId(slug, userId)
      _ <- Logger[F].trace(s"User [$userId] deleted article with slug [$slug] successfully")
    } yield ()

  def favoriteArticleBySlug(slug: Slug, userId: UserId): F[Unit] =
    for {
      _       <- Logger[F].trace(s"User [$userId] is trying to favorite article with slug [$slug]")
      article <- findArticleBySlug(slug)
      _       <- articlesRepositoryAlgebra.favorite(article.id, userId)
      _       <- Logger[F].trace(s"User [$userId] successfully favorited article with slug [$slug]")
    } yield ()

  def unfavoriteArticleBySlug(slug: Slug, userId: UserId): F[Unit] =
    for {
      _       <- Logger[F].trace(s"User [$userId] is trying to unfavorite article with slug [$slug]")
      article <- findArticleBySlug(slug)
      _       <- articlesRepositoryAlgebra.unfavorite(article.id, userId)
      _       <- Logger[F].trace(s"User [$userId] successfully unfavorited article with slug [$slug]")
    } yield ()

  def isFavoritedByUser(articleId: ArticleId, userId: UserId): F[IsFavorited] =
    for {
      _           <- Logger[F].trace(s"Checking if user [$userId] is followed article [$articleId]")
      isFavorited <- articlesRepositoryAlgebra.isFavoritedByUser(articleId, userId).map(IsFavorited(_))
    } yield isFavorited

  def findFavoritedCount(articleId: ArticleId): F[FavoritesCount] =
    articlesRepositoryAlgebra
      .findFavoritedRecordsByArticleId(articleId)
      .map(favoritedRecordsList => FavoritesCount(favoritedRecordsList.size))

  def fetchArticle(slug: Slug, optionalUserId: Option[UserId]): F[(Article, Profile, List[Tag], IsFavorited, FavoritesCount)] =
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
  ): F[List[(Article, Profile, List[Tag], IsFavorited, FavoritesCount)]] =
    articlesRepositoryAlgebra
      .listArticles(maybeAuthorUsername, maybeFavoritedAuthorUsername, maybeTagName, pagination)
      .flatMap(toArticleWithMetadata(_, maybeUserId))

  def feed(userId: UserId, pagination: Pagination): F[List[(Article, Profile, List[Tag], IsFavorited, FavoritesCount)]] =
    for {
      _    <- Logger[F].trace(s"Retrieving feed for user [$userId]")
      feed <- articlesRepositoryAlgebra.listArticlesByFavoritedUsers(userId, pagination).flatMap(toArticleWithMetadata(_, Some(userId)))
      _    <- Logger[F].trace(s"Found [${feed.length}] of articles on user [$userId]s feed")
    } yield feed

  def findFavoritedsByUserId(articleIds: NonEmptyList[ArticleId], userId: Option[UserId]): F[Set[ArticleId]] =
    userId.map(userId => articlesRepositoryAlgebra.findFavoritedsByUserId(articleIds, userId)).getOrElse(Set.empty[ArticleId].pure[F])

  private def toArticleWithMetadata(
      articles: List[Article],
      userId: Option[UserId]
  ): F[List[(Article, Profile, List[Tag], IsFavorited, FavoritesCount)]] =
    NonEmptyList.fromList(articles) match {
      case Some(articles) =>
        retrieveMetadata(userId, articles).map(_.toList)
      case None => List.empty[(Article, Profile, List[Tag], IsFavorited, FavoritesCount)].pure[F]
    }

  private def retrieveMetadata(
      maybeUserId: Option[UserId],
      articles: NonEmptyList[Article]
  ): F[NonEmptyList[(Article, Profile, List[Tag], IsFavorited, FavoritesCount)]] = {
    val nelArticleIds: NonEmptyList[ArticleId] = articles.map(_.id)
    for {
      profiles              <- userService.findProfilesByUserId(articles.map(_.authorId), maybeUserId)
      tags                  <- tagService.findTagsByArticleIds(nelArticleIds)
      isFavoriteds          <- findFavoritedsByUserId(nelArticleIds, maybeUserId)
      articleFavoriteCounts <- articlesRepositoryAlgebra.findArticlesFavoriteCount(nelArticleIds)
    } yield {
      articles.map { article =>
        val profile     = profiles(article.authorId) // I dont use transactions and this can throw
        val articleTags = tags.getOrElse(article.id, List.empty[Tag]) // Todo test your test setup to see exact error
        // val articleTags    = tags(article.id)
        val isFavorited    = IsFavorited(isFavoriteds.contains(article.id))
        val favoritedCount = articleFavoriteCounts.getOrElse(article.id, FavoritesCount(0))
        (article, profile, articleTags, isFavorited, favoritedCount)
      }
    }
  }
}

object ArticleService {

  def apply[F[_]: Async: Logger]()(
      implicit articlesRepositoryAlgebra: ArticleRepositoryAlgebra[F],
      userService: UserService[F],
      tagService: TagService[F],
      followerService: FollowerService[F]
  ): ArticleService[F] =
    new ArticleService()
}
