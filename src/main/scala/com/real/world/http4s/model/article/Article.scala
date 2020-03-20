package com.real.world.http4s.model.article

import java.time.Instant
import java.util.UUID

import Article.{ ArticleBody, ArticleId, Description, Slug, Title }
import com.real.world.http4s.model.user.User.UserId

import io.circe.Codec
import io.circe.generic.extras.semiauto.deriveUnwrappedCodec

final case class Article(
    id: ArticleId,
    slug: Slug,
    title: Title,
    description: Description,
    body: ArticleBody,
    createdAt: Instant,
    updatedAt: Instant,
    authorId: UserId
)

// ToDo dont use coded everywhere!
object Article {

  final case class ArticleId(value: Int) extends AnyVal
  final case class Title(value: String) extends AnyVal
  final case class Slug(value: String) extends AnyVal
  final case class Description(value: String) extends AnyVal
  final case class ArticleBody(value: String) extends AnyVal
  final case class FavoritesCount(value: Int) extends AnyVal

  implicit val ArticleIdCodec: Codec[ArticleId]           = deriveUnwrappedCodec[ArticleId]
  implicit val TitleCodec: Codec[Title]                   = deriveUnwrappedCodec[Title]
  implicit val SlugCodec: Codec[Slug]                     = deriveUnwrappedCodec[Slug]
  implicit val DescriptionCodec: Codec[Description]       = deriveUnwrappedCodec[Description]
  implicit val ArticleBodyCodec: Codec[ArticleBody]       = deriveUnwrappedCodec[ArticleBody]
  implicit val FavoritesCountCodec: Codec[FavoritesCount] = deriveUnwrappedCodec[FavoritesCount]

  def apply(
      id: ArticleId,
      title: Title,
      description: Description,
      body: ArticleBody,
      createdAt: Instant,
      updatedAt: Instant,
      authorId: UserId
  ): Article = new Article(id, title.toSlug, title, description, body, createdAt, updatedAt, authorId)

  // Implicit classes
  implicit class StringToSlug(title: Title) {
    def toSlug: Slug = Slug(slugify(title.value))
    private def slugify(input: String): String = {
      import java.text.Normalizer
      Normalizer
        .normalize(input, Normalizer.Form.NFD)
        .replaceAll("[^\\w\\s-]", "") // Remove all non-word, non-space or non-dash characters
        .replace('-', ' ') // Replace dashes with spaces
        .trim // Trim leading/trailing whitespace (including what used to be leading/trailing dashes)
        .replaceAll("\\s+", "-") // Replace whitespace (including newlines and repetitions) with single dashes
        .toLowerCase // Lowercase the final results
        .concat(s"-${UUID.randomUUID.toString}")
    }
  }

}
