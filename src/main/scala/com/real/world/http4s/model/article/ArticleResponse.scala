package com.real.world.http4s.model.article

import java.time.Instant

import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.semiauto.deriveEncoder
import io.circe.refined._
import io.circe.{ Decoder, Encoder }

import com.real.world.http4s.model.Instances._
import com.real.world.http4s.model._
import com.real.world.http4s.model.profile.Profile
import com.real.world.http4s.model.tag.{ Tag, TagOut }

final case class ArticleResponse(
    slug: Slug,
    title: Title,
    description: Description,
    body: ArticleBody,
    tagList: List[TagOut],
    createdAt: Instant,
    updatedAt: Instant,
    favorited: IsFavorited,
    favoritesCount: FavoritesCount,
    author: Profile
)

final case class ArticleResponseWrapper(article: ArticleResponse)
final case class ArticleResponseListWrapper(articles: List[ArticleResponse], articlesCount: Int)

object ArticleResponse {

  implicit val ArticleResponseOutEncoder: Encoder[ArticleResponse] = deriveEncoder[ArticleResponse]
  implicit val ArticleResponseOutDecoder: Decoder[ArticleResponse] = deriveDecoder[ArticleResponse]

  def apply(
      article: Article,
      author: Profile,
      tags: List[TagOut],
      favorited: IsFavorited,
      favoritesCount: FavoritesCount
  ): ArticleResponse =
    new ArticleResponse(
      article.slug,
      article.title,
      article.description,
      article.body,
      tags,
      article.createdAt,
      article.updatedAt,
      favorited,
      favoritesCount,
      author
    )
}

object ArticleResponseWrapper {
  implicit val ArticleResponseOutWrapperEncoder: Encoder[ArticleResponseWrapper] = deriveEncoder[ArticleResponseWrapper]

  def apply(
      article: Article,
      author: Profile,
      tags: List[Tag],
      favorited: IsFavorited,
      favoritesCount: FavoritesCount
  ): ArticleResponseWrapper =
    new ArticleResponseWrapper(
      ArticleResponse(
        article        = article,
        author         = author,
        tags           = tags.map(TagOut.fromTag),
        favorited      = favorited,
        favoritesCount = favoritesCount
      )
    )

  def create: (Article, Profile, List[Tag], IsFavorited, FavoritesCount) => ArticleResponseWrapper = apply

}

object ArticleResponseListWrapper {
  implicit val ArticleListResponseWrapperEncoder: Encoder[ArticleResponseListWrapper] = deriveEncoder[ArticleResponseListWrapper]

  def apply(articlesWithMetadata: => List[(Article, Profile, List[Tag], IsFavorited, FavoritesCount)]): ArticleResponseListWrapper = {
    val articles: List[ArticleResponse] = articlesWithMetadata.map {
      case (article, profile, tags, isFavorited, favoriteCount) =>
        ArticleResponse(
          article        = article,
          author         = profile,
          tags           = tags.map(TagOut.fromTag),
          favorited      = isFavorited,
          favoritesCount = favoriteCount
        )
    }
    new ArticleResponseListWrapper(articles, articles.size)
  }

}
