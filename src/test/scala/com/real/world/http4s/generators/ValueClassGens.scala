package com.real.world.http4s.generators

import scala.collection.immutable.Stream
import scala.util.Random

import com.real.world.http4s.model.comment.Comment.CommentId
import com.real.world.http4s.model.tag.TagIn
import com.real.world.http4s.model.{ Username, Title, ArticleBody, Description, Email, PlainTextPassword, ArticleId, Bio, UserId, Image, CommentBody }

import eu.timepit.refined.types.numeric.{ NonNegInt, PosInt }
import eu.timepit.refined.types.string.NonEmptyString
import org.scalacheck.Gen

// scalastyle:off magic.number
trait ValueClassGens {

  private def alphanumeric: Stream[Char] = {
    def nextAlphaNum: Char = {
      val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
      chars charAt (Random nextInt chars.length)
    }
    Stream continually nextAlphaNum
  }

  private def numbers: Stream[Char] = {
    def nextAlphaNum: Char = {
      val chars = "0123456789"
      chars charAt (Random nextInt chars.length)
    }
    Stream continually nextAlphaNum
  }

  private def randomString: String =
    Random.alphanumeric.take(Random.nextInt(36) + 15).mkString

  private def randomPassword: String =
    Random
      .shuffle(
        (alphanumeric.take(2).mkString.toLowerCase
        + alphanumeric.take(2).mkString.toUpperCase
        + numbers.take(2).mkString).toList
      )
      .mkString

  private def randomUsername: String =
    Random
      .shuffle(
        (alphanumeric.take(2).mkString.toLowerCase
        + alphanumeric.take(2).mkString.toUpperCase
        + numbers.take(2).mkString).toList
      )
      .mkString

  private def randomSmallString: String = Random.alphanumeric.take(Random.nextInt(10) + 1).mkString
  private def randomBigString: String   = Random.alphanumeric.take(Random.nextInt(256) + 256).mkString
  private def randomId: Int             = Random.nextInt(Int.MaxValue) + 1

  protected def emailGen: Gen[Email] =
    Gen.delay(s"$randomSmallString@$randomSmallString.com").map(v => Email(NonEmptyString.unsafeFrom(v)))
  protected def passwordGen: Gen[PlainTextPassword] = Gen.delay(randomPassword).map(v => PlainTextPassword(NonEmptyString.unsafeFrom(v)))
  protected def usernameGen: Gen[Username]          = Gen.delay(randomUsername).map(v => Username(NonEmptyString.unsafeFrom(v)))
  protected def userIDGen: Gen[UserId]              = Gen.delay(UserId(NonNegInt.unsafeFrom(randomId)))
  protected def bioGen: Gen[Option[Bio]]            = Gen.delay(Some(Bio(NonEmptyString.unsafeFrom(randomString))))
  protected def imageGen: Gen[Option[Image]]        = Gen.delay(Some(Image(NonEmptyString.unsafeFrom(randomString))))
  protected def commentIdGen: Gen[CommentId]        = Gen.delay(CommentId(PosInt.unsafeFrom(randomId)))
  protected def commentBodyGen: Gen[CommentBody]    = Gen.delay(CommentBody(NonEmptyString.unsafeFrom(randomString)))
  protected def articleIdGen: Gen[ArticleId]        = Gen.delay(ArticleId(NonNegInt.unsafeFrom(randomId)))
  protected def articleBodyGen: Gen[ArticleBody]    = Gen.delay(ArticleBody(NonEmptyString.unsafeFrom(randomString)))
  protected def titleGen: Gen[Title]                = Gen.delay(Title(NonEmptyString.unsafeFrom(randomString)))
  protected def descriptionGen: Gen[Description]    = Gen.delay(Description(NonEmptyString.unsafeFrom(randomString)))
  protected def tagInGen: Gen[TagIn]                = Gen.delay(TagIn(randomString))

}
// scalastyle:on magic.number
