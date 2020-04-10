package com.real.world.http4s.repository

import java.time.Instant
import java.util.Date

import doobie.quill.DoobieContext

import com.real.world.http4s.model.comment.Comment.CommentId
import com.real.world.http4s.model.{ Slug, Title, ArticleBody, Description, ArticleId, UserId, CommentBody }

import eu.timepit.refined.types.numeric.{ PosInt, NonNegInt }
import eu.timepit.refined.types.string.NonEmptyString
import io.getquill.Literal

trait QuillSupport {
  val DoobiePostgresContext = new DoobieContext.Postgres(Literal)
  import DoobiePostgresContext._

  implicit val instanceEncoder: MappedEncoding[Instant, Date] = MappedEncoding[Instant, Date](Date.from)
  implicit val instantDecoder: MappedEncoding[Date, Instant]  = MappedEncoding[Date, Instant](_.toInstant)

  implicit val articleIdEncoder: MappedEncoding[ArticleId, Int] = MappedEncoding[ArticleId, Int](_.value.value)
  implicit val articleIdDecoder: MappedEncoding[Int, ArticleId] = MappedEncoding[Int, ArticleId](v => ArticleId(NonNegInt.unsafeFrom(v)))

  implicit val userIdEncoder: MappedEncoding[UserId, Int] = MappedEncoding[UserId, Int](_.value.value)
  implicit val userIdDecoder: MappedEncoding[Int, UserId] = MappedEncoding[Int, UserId](v => UserId(NonNegInt.unsafeFrom(v)))

  implicit val slugEncoder: MappedEncoding[Slug, String] = MappedEncoding[Slug, String](_.value.value)
  implicit val slugDecoder: MappedEncoding[String, Slug] = MappedEncoding[String, Slug](v => Slug(NonEmptyString.unsafeFrom(v)))

  implicit val commentIdEncoder: MappedEncoding[CommentId, Int] = MappedEncoding[CommentId, Int](_.value.value)
  implicit val commentIdDecoder: MappedEncoding[Int, CommentId] = MappedEncoding[Int, CommentId](v => CommentId(PosInt.unsafeFrom(v)))

  implicit val commentBodyEncoder: MappedEncoding[CommentBody, String] = MappedEncoding[CommentBody, String](_.value.value)
  implicit val commentBodyDecoder: MappedEncoding[String, CommentBody] =
    MappedEncoding[String, CommentBody](v => CommentBody(NonEmptyString.unsafeFrom(v)))

  implicit val titleEncoder: MappedEncoding[Title, String] = MappedEncoding[Title, String](_.value.value)
  implicit val titleDecoder: MappedEncoding[String, Title] =
    MappedEncoding[String, Title](v => Title(NonEmptyString.unsafeFrom(v)))

  implicit val descEncoder: MappedEncoding[Description, String] = MappedEncoding[Description, String](_.value.value)
  implicit val descDecoder: MappedEncoding[String, Description] =
    MappedEncoding[String, Description](v => Description(NonEmptyString.unsafeFrom(v)))

  implicit val articleBodyEncoder: MappedEncoding[ArticleBody, String] = MappedEncoding[ArticleBody, String](_.value.value)
  implicit val articleBodyDecoder: MappedEncoding[String, ArticleBody] =
    MappedEncoding[String, ArticleBody](v => ArticleBody(NonEmptyString.unsafeFrom(v)))

}

object QuillSupport extends QuillSupport
