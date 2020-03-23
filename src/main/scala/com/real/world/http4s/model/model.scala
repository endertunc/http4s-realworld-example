package com.real.world.http4s

// import io.circe.refined._
// import io.circe.{ Encoder, Decoder }
// import doobie.refined.implicits._
// import doobie.util.meta.Meta
import java.util.UUID

import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
// import scalaz.annotation.deriving

package object model {

  /* @deriving(Meta, Decoder, Encoder) */
  @newtype case class ArticleId(value: NonNegInt)
  @newtype case class Title(value: NonEmptyString) {
    def toSlug: Slug = Slug(NonEmptyString.unsafeFrom(Title.slugify(value.value)))
  }
  @newtype case class Slug(value: NonEmptyString)
  @newtype case class Description(value: NonEmptyString)
  @newtype case class ArticleBody(value: NonEmptyString)
  @newtype case class FavoritesCount(value: NonNegInt)
  @newtype case class CommentBody(value: NonEmptyString)
  @newtype case class UserId(value: NonNegInt)
  @newtype case class Username(value: NonEmptyString)
  @newtype case class PlainTextPassword(value: NonEmptyString)
  @newtype case class HashedPassword(value: String)
  @newtype case class Email(value: NonEmptyString)
  @newtype case class Bio(value: NonEmptyString)
  @newtype case class Image(value: NonEmptyString)
  @newtype case class Token(value: NonEmptyString)

//  abstract class RefinedTypeOps[FTP, T, M](toModel: FTP => M)(implicit rt: RefinedType.AuxT[FTP, T]) {
//    def from(t: T): Either[String, M] = rt.refine(t).map(toModel(_))
//    def unsafeFrom(t: T): M           = toModel(rt.unsafeRefine(t))
//  }

  object Username {
    def from(username: String): Either[String, Username] = NonEmptyString.from(username).map(Username(_))
    def unsafeFrom(username: String): Username           = Username(NonEmptyString.unsafeFrom(username))
  }

  object Token {
    def unsafeFrom(token: String): Token = Token(NonEmptyString.unsafeFrom(token))
  }

  object Title {
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

  val zeroFavorites: FavoritesCount = FavoritesCount(NonNegInt.MinValue)

}
