package com.real.world.http4s.model.article

import cats.effect.Sync
import cats.implicits._

import com.real.world.http4s.AppError.InvalidFieldError
import com.real.world.http4s.model.ValidationErrors.{ FieldValidationResult, InvalidField }
import com.real.world.http4s.model.tag.Tag.TagName
import com.real.world.http4s.model.{ ArticleBody, Description, Title }

import io.chrisdavenport.log4cats.Logger

trait ArticleInputValidator {

  protected def validateTitle(title: String): FieldValidationResult[Title] =
    Title.from(title).fold(_ => InvalidField("title", "Todo").invalidNec, _.validNec)

  protected def validateDescription(description: String): FieldValidationResult[Description] =
    Description.from(description).fold(_ => InvalidField("description", "Todo").invalidNec, _.validNec)

  // ToDo double check err.getMessage
  protected def validateArticleBody(body: String): FieldValidationResult[ArticleBody] =
    ArticleBody.from(body).fold(_ => InvalidField("body", "Todo").invalidNec, _.validNec)

  protected def validateArticleTags(tags: List[String]): FieldValidationResult[List[TagName]] =
    tags.map(tag => TagName.from(tag).fold(_ => InvalidField("tag", "Todo").invalidNec, _.validNec)).sequence

  def validateCreateArticleInput[F[_]: Sync: Logger](createArticleInput: CreateArticleInput): F[CreateArticleRequest] = {
    import createArticleInput._
    (
      validateTitle(title),
      validateDescription(description),
      validateArticleBody(body),
      tagList.traverse(validateArticleTags)
    ).tupled.fold(
      InvalidFieldError(_).raiseError[F, CreateArticleRequest], {
        case (title, description, body, tags) => CreateArticleRequest(title, description, body, tags).pure[F]
      }
    )
  }

  def validateUpdateArticleInput[F[_]: Sync: Logger](updateArticleInput: UpdateArticleInput): F[UpdateArticleRequest] = {
    import updateArticleInput._
    (
      title.traverse(validateTitle),
      description.traverse(validateDescription),
      body.traverse(validateArticleBody)
    ).tupled.fold(
      InvalidFieldError(_).raiseError[F, UpdateArticleRequest], {
        case (title, description, body) => UpdateArticleRequest(title, description, body).pure[F]
      }
    )
  }

}
