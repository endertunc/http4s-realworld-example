package com.real.world.http4s

import java.util.UUID

import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.And
import eu.timepit.refined.boolean._
import eu.timepit.refined.collection.MaxSize
import eu.timepit.refined.collection.MinSize
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.collection._
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.string.Trimmed
import eu.timepit.refined.string.Url
import eu.timepit.refined.string._
import eu.timepit.refined.types.numeric.NonNegInt
import io.estatico.newtype.macros.newtype

// SCoverage does not work well with newtype
// $COVERAGE-OFF$
package object model {

  type ArticleIdAux = NonNegative
  @newtype case class ArticleId(value: Int Refined ArticleIdAux)
  object ArticleId {
    def from(v: Int): Either[String, ArticleId] = refineV[ArticleIdAux](v).map(ArticleId(_))
    def unsafeFrom(v: Int): ArticleId           = ArticleId(refineV[ArticleIdAux].unsafeFrom(v))

  }

  type SlugAux = NonEmpty
  @newtype case class Slug(value: String Refined SlugAux)
  object Slug {
    def from(v: String): Either[String, Slug] = refineV[SlugAux](v).map(Slug(_))
    def unsafeFrom(v: String): Slug           = Slug(refineV[SlugAux].unsafeFrom(v))
  }

  type DescriptionAux = NonEmpty
  @newtype case class Description(value: String Refined DescriptionAux)
  object Description {
    def from(v: String): Either[String, Description] = refineV[DescriptionAux](v).map(Description(_))
    def unsafeFrom(v: String): Description           = Description(refineV[DescriptionAux].unsafeFrom(v))
  }

  type ArticleBodyAux = NonEmpty
  @newtype case class ArticleBody(value: String Refined ArticleBodyAux)
  object ArticleBody {
    def from(v: String): Either[String, ArticleBody] = refineV[ArticleBodyAux](v).map(ArticleBody(_))
    def unsafeFrom(v: String): ArticleBody           = ArticleBody(refineV[ArticleBodyAux].unsafeFrom(v))
  }

  type FavoritesCountAux = NonNegative
  @newtype case class FavoritesCount(value: Int Refined FavoritesCountAux)
  object FavoritesCount {
    def from(v: Int): Either[String, FavoritesCount] = refineV[FavoritesCountAux](v).map(FavoritesCount(_))
    def unsafeFrom(v: Int): FavoritesCount           = FavoritesCount(refineV[FavoritesCountAux].unsafeFrom(v))
    val zeroFavorite: FavoritesCount                 = FavoritesCount(NonNegInt.MinValue)

  }

  type CommentBodyAux = NonEmpty
  @newtype case class CommentBody(value: String Refined CommentBodyAux)
  object CommentBody {
    def from(v: String): Either[String, CommentBody] = refineV[CommentBodyAux](v).map(CommentBody(_))
    def unsafeFrom(v: String): CommentBody           = CommentBody(refineV[CommentBodyAux].unsafeFrom(v))
  }

  type UserIdAux = NonNegative
  @newtype case class UserId(value: Int Refined UserIdAux)
  object UserId {
    def from(v: Int): Either[String, UserId] = refineV[UserIdAux](v).map(UserId(_))
    def unsafeFrom(v: Int): UserId           = UserId(refineV[UserIdAux].unsafeFrom(v))
  }

  type UsernameAux = Trimmed And MinSize[W.`3`.T] And MaxSize[W.`30`.T]
  @newtype case class Username(value: String Refined UsernameAux)
  object Username {
    def from(v: String): Either[String, Username] = refineV[UsernameAux](v).map(Username(_))
    def unsafeFrom(v: String): Username           = Username(refineV[UsernameAux].unsafeFrom(v))
  }

  type PlainTextPasswordAux = Trimmed And MinSize[W.`3`.T] And MaxSize[W.`20`.T]
  @newtype case class PlainTextPassword(value: String Refined UsernameAux)
  object PlainTextPassword {
    def from(v: String): Either[String, PlainTextPassword] = refineV[UsernameAux](v).map(PlainTextPassword(_))
    def unsafeFrom(v: String): PlainTextPassword           = PlainTextPassword(refineV[UsernameAux].unsafeFrom(v))
  }

  type HashedPasswordAux = NonEmpty
  @newtype case class HashedPassword(value: String Refined HashedPasswordAux)
  object HashedPassword {
    def from(v: String): Either[String, HashedPassword] = refineV[HashedPasswordAux](v).map(HashedPassword(_))
    def unsafeFrom(v: String): HashedPassword           = HashedPassword(refineV[HashedPasswordAux].unsafeFrom(v))
  }

  // The only way to validate an email is to send a confirmation email
  // using regex's on email addresses should be with helping the user prevent typos
  type EmailAux = MatchesRegex[W.`"""^[^@\\s]+@[^@\\s\\.]+\\.[^@\\.\\s]+$"""`.T]
  @newtype case class Email(value: String Refined EmailAux)
  object Email {
    def from(v: String): Either[String, Email] = refineV[EmailAux](v).map(Email(_))
    def unsafeFrom(v: String): Email           = Email(refineV[EmailAux].unsafeFrom(v))
  }

  type BioAux = Trimmed And MinSize[W.`1`.T] And MaxSize[W.`255`.T]
  @newtype case class Bio(value: String Refined BioAux)
  object Bio {
    def from(v: String): Either[String, Bio] = refineV[BioAux](v).map(Bio(_))
    def unsafeFrom(v: String): Bio           = Bio(refineV[BioAux].unsafeFrom(v))
  }

  type ImageAux = Url
  @newtype case class Image(value: String Refined ImageAux)
  object Image {
    def from(v: String): Either[String, Image] = refineV[ImageAux](v).map(Image(_))
    def unsafeFrom(v: String): Image           = Image(refineV[ImageAux].unsafeFrom(v))
  }

  type TokenAux = NonEmpty
  @newtype case class Token(value: String Refined TokenAux)
  object Token {
    def from(v: String): Either[String, Token] = refineV[TokenAux](v).map(Token(_))
    def unsafeFrom(v: String): Token           = Token(refineV[TokenAux].unsafeFrom(v))
  }

  type TitleAux = Trimmed And MinSize[W.`1`.T] And MaxSize[W.`255`.T]
  @newtype case class Title(value: String Refined TitleAux) {
    def toSlug: Slug = Slug.unsafeFrom(Title.slugify(value))
  }

  object Title {
    def from(v: String): Either[String, Title] = refineV[TitleAux](v).map(Title(_))
    def unsafeFrom(v: String): Title           = Title(refineV[TitleAux].unsafeFrom(v))
    private def slugify(slug: String Refined TitleAux): String = {
      import java.text.Normalizer
      Normalizer
        .normalize(slug.value, Normalizer.Form.NFD)
        .replaceAll("[^\\w\\s-]", "") // Remove all non-word, non-space or non-dash characters
        .replace('-', ' ') // Replace dashes with spaces
        .trim // Trim leading/trailing whitespace (including what used to be leading/trailing dashes)
        .replaceAll("\\s+", "-") // Replace whitespace (including newlines and repetitions) with single dashes
        .toLowerCase // Lowercase the final results
        .concat(s"-${UUID.randomUUID.toString}")
    }
  }

}
// $COVERAGE-ON$
