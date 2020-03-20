package com.real.world.http4s.json

import com.real.world.http4s.model.article.Article.{ ArticleBody, Description, Title }
import com.real.world.http4s.model.tag.TagIn
import com.real.world.http4s.model.user.User.{ Bio, Email, Image, PlainTextPassword, Username }
import com.real.world.http4s.model.article.Article.{ ArticleBody, Description, Title }
import com.real.world.http4s.model.tag.TagIn
import com.real.world.http4s.model.user.User.{ Bio, Email, Image, PlainTextPassword, Username }

object ValueClassSchemaValidators extends ValueClassSchemaValidators

trait ValueClassSchemaValidators {

  // user & profile
  implicit val emailSchema: json.Schema[Email]                = json.Json.schema[Email]("email")
  implicit val passwordSchema: json.Schema[PlainTextPassword] = json.Json.schema[PlainTextPassword]("plainTextPassword")
  implicit val usernameSchema: json.Schema[Username]          = json.Json.schema[Username]("username")
  implicit val bioSchema: json.Schema[Bio]                    = json.Json.schema[Bio]("bio")
  implicit val imageSchema: json.Schema[Image]                = json.Json.schema[Image]("image")

  // article
  implicit val titleSchema: json.Schema[Title]             = json.Json.schema[Title]("email")
  implicit val descriptionSchema: json.Schema[Description] = json.Json.schema[Description]("description")
  implicit val articleBodySchema: json.Schema[ArticleBody] = json.Json.schema[ArticleBody]("body")
  implicit val tagInSchema: json.Schema[TagIn]             = json.Json.schema[TagIn]("tagIn")
  implicit val tagListSchema: json.Schema[List[TagIn]]     = json.Json.schema[List[TagIn]]("tagList")

}
