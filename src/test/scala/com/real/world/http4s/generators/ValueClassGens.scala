package com.real.world.http4s.generators

import com.real.world.http4s.model.comment.Comment.{ CommentBody, CommentId }
import com.real.world.http4s.model.article.Article.{ ArticleBody, ArticleId, Description, Title }
import com.real.world.http4s.model.tag.TagIn
import com.real.world.http4s.model.user.User.{ Bio, Email, Image, PlainTextPassword, UserId, Username }

import scala.collection.immutable.Stream
import scala.util.Random
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

  protected def emailGen: Gen[Email]                = Gen.delay(s"$randomSmallString@$randomSmallString.com").map(Email)
  protected def passwordGen: Gen[PlainTextPassword] = Gen.delay(randomPassword).map(PlainTextPassword)
  protected def usernameGen: Gen[Username]          = Gen.delay(randomUsername).map(Username)
  protected def userIDGen: Gen[UserId]              = Gen.delay(UserId(randomId))
  protected def bioGen: Gen[Option[Bio]]            = Gen.delay(Some(Bio(randomString)))
  protected def imageGen: Gen[Option[Image]]        = Gen.delay(Some(Image(randomString)))
  protected def commentIdGen: Gen[CommentId]        = Gen.delay(CommentId(randomId))
  protected def commentBodyGen: Gen[CommentBody]    = Gen.delay(randomString).map(CommentBody)
  protected def articleIdGen: Gen[ArticleId]        = Gen.delay(ArticleId(randomId))
  protected def articleBodyGen: Gen[ArticleBody]    = Gen.delay(randomBigString).map(ArticleBody)
  protected def titleGen: Gen[Title]                = Gen.delay(randomString).map(Title)
  protected def descriptionGen: Gen[Description]    = Gen.delay(randomString).map(Description)
  protected def tagInGen: Gen[TagIn]                = Gen.delay(randomString).map(TagIn(_))

}
// scalastyle:on magic.number
