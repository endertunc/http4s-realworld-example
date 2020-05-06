package com.real.world.http4s.generators

import scala.collection.immutable.Stream
import scala.util.Random

import com.real.world.http4s.model.comment.Comment.CommentId
import com.real.world.http4s.model.tag.Tag.TagName
import com.real.world.http4s.model.{ ArticleBody, ArticleId, Bio, CommentBody, Description, Email, Image, PlainTextPassword, Title, UserId, Username }

import org.scalacheck.{ Arbitrary, Gen }

// scalastyle:off magic.number
trait GeneratorsBase {

  def emailGen: Gen[Email]                = shortAlphaString.map(s => Email.unsafeFrom(s"$s@realworld.com"))
  def passwordGen: Gen[PlainTextPassword] = passwordStringGen.map(PlainTextPassword.unsafeFrom)
  def usernameGen: Gen[Username]          = usernameStringGen.map(Username.unsafeFrom)
  def userIDGen: Gen[UserId]              = positiveInt.map(UserId.unsafeFrom)
  def bioGen: Gen[Option[Bio]]            = shortAlphaString.map(s => Option(Bio.unsafeFrom(s)))
  def imageGen: Gen[Option[Image]]        = shortAlphaString.map(s => Option(Image.unsafeFrom(s"https://$s.com")))
  def commentIdGen: Gen[CommentId]        = positiveInt.map(CommentId.unsafeFrom)
  def commentBodyGen: Gen[CommentBody]    = shortAlphaString.map(CommentBody.unsafeFrom)
  def articleIdGen: Gen[ArticleId]        = positiveInt.map(ArticleId.unsafeFrom)
  def articleBodyGen: Gen[ArticleBody]    = shortAlphaString.map(ArticleBody.unsafeFrom)
  def titleGen: Gen[Title]                = shortAlphaString.map(Title.unsafeFrom)
  def descriptionGen: Gen[Description]    = shortAlphaString.map(Description.unsafeFrom)
  def tagNameGen: Gen[TagName]            = shortAlphaString.map(TagName.unsafeFrom)

  def shortAlphaString: Gen[String] = Gen.alphaNumStr.retryUntil(str => 3 <= str.length && str.length <= 15)
  def positiveInt: Gen[Int]         = Gen.chooseNum(1, 10000)
  def arbNegativeInt: Gen[Int]      = Gen.chooseNum(-10000, 0)

  def usernameStringGen: Gen[String] =
    for {
      alphaLowerCase <- Gen.alphaStr.map(_.toLowerCase).retryUntil(_.length == 2)
      alphaUpperCase <- Gen.alphaStr.map(_.toUpperCase).retryUntil(_.length == 2)
      number         <- Gen.numStr.retryUntil(_.length == 2)
    } yield Random.shuffle((alphaLowerCase + alphaUpperCase + number).toList).mkString

  def passwordStringGen: Gen[String] =
    for {
      alphaLowerCase <- Gen.alphaStr.map(_.toLowerCase).retryUntil(_.length == 3)
      alphaUpperCase <- Gen.alphaStr.map(_.toUpperCase).retryUntil(_.length == 3)
      number         <- Gen.numStr.retryUntil(_.length == 3)
    } yield Random.shuffle((alphaLowerCase + alphaUpperCase + number).toList).mkString

}
// scalastyle:on magic.number
