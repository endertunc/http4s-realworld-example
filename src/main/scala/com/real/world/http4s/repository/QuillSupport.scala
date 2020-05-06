package com.real.world.http4s.repository

import java.time.Instant
import java.util.Date

import doobie.quill.DoobieContext

import com.real.world.http4s.model.comment.Comment.CommentId
import com.real.world.http4s.model.tag.Tag.{ TagId, TagName }
import com.real.world.http4s.model.{ ArticleBody, ArticleId, CommentBody, Description, Slug, Title, UserId }

import io.getquill.Literal

trait QuillSupport {
  val DoobiePostgresContext = new DoobieContext.Postgres(Literal)
  import DoobiePostgresContext._

  implicit val instanceEncoder: MappedEncoding[Instant, Date] = MappedEncoding[Instant, Date](Date.from)
  implicit val instantDecoder: MappedEncoding[Date, Instant]  = MappedEncoding[Date, Instant](_.toInstant)

  implicit val articleIdEncoder: MappedEncoding[ArticleId, Int] = MappedEncoding[ArticleId, Int](_.value.value)
  implicit val articleIdDecoder: MappedEncoding[Int, ArticleId] = MappedEncoding[Int, ArticleId](ArticleId.unsafeFrom(_))

  implicit val userIdEncoder: MappedEncoding[UserId, Int] = MappedEncoding[UserId, Int](_.value.value)
  implicit val userIdDecoder: MappedEncoding[Int, UserId] = MappedEncoding[Int, UserId](UserId.unsafeFrom(_))

  implicit val slugEncoder: MappedEncoding[Slug, String] = MappedEncoding[Slug, String](_.value.value)
  implicit val slugDecoder: MappedEncoding[String, Slug] = MappedEncoding[String, Slug](Slug.unsafeFrom(_))

  implicit val commentIdEncoder: MappedEncoding[CommentId, Int] = MappedEncoding[CommentId, Int](_.value.value)
  implicit val commentIdDecoder: MappedEncoding[Int, CommentId] = MappedEncoding[Int, CommentId](CommentId.unsafeFrom(_))

  implicit val commentBodyEncoder: MappedEncoding[CommentBody, String] = MappedEncoding[CommentBody, String](_.value.value)
  implicit val commentBodyDecoder: MappedEncoding[String, CommentBody] = MappedEncoding[String, CommentBody](CommentBody.unsafeFrom(_))

  implicit val titleEncoder: MappedEncoding[Title, String] = MappedEncoding[Title, String](_.value.value)
  implicit val titleDecoder: MappedEncoding[String, Title] = MappedEncoding[String, Title](Title.unsafeFrom(_))

  implicit val descEncoder: MappedEncoding[Description, String] = MappedEncoding[Description, String](_.value.value)
  implicit val descDecoder: MappedEncoding[String, Description] = MappedEncoding[String, Description](Description.unsafeFrom(_))

  implicit val articleBodyEncoder: MappedEncoding[ArticleBody, String] = MappedEncoding[ArticleBody, String](_.value.value)
  implicit val articleBodyDecoder: MappedEncoding[String, ArticleBody] = MappedEncoding[String, ArticleBody](ArticleBody.unsafeFrom(_))

  implicit val tagIdEncoder: MappedEncoding[TagId, Int] = MappedEncoding[TagId, Int](_.value.value)
  implicit val tagIdDecoder: MappedEncoding[Int, TagId] = MappedEncoding[Int, TagId](TagId.unsafeFrom(_))

  implicit val tagNameEncoder: MappedEncoding[TagName, String] = MappedEncoding[TagName, String](_.value.value)
  implicit val tagNameDecoder: MappedEncoding[String, TagName] = MappedEncoding[String, TagName](TagName.unsafeFrom(_))

}

object QuillSupport extends QuillSupport
