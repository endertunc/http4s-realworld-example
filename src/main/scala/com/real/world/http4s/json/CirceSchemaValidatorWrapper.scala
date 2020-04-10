package com.real.world.http4s.json

import cats.data.{ NonEmptyList, ValidatedNel }
import cats.effect.Sync

import io.circe.Json
import io.circe.schema.ValidationError

// ToDo This class also can be abstracted so we dont have direct dependency to circe but hey circe is everywhere anyway
class CirceSchemaValidatorWrapper[T](id: String)(implicit schema: json.Schema[T]) extends io.circe.schema.Schema {
  import com.github.andyglow.jsonschema.AsCirce._
  import json.schema.Version._
  val _schema: io.circe.schema.Schema = io.circe.schema.Schema.load(schema.asCirce(Draft07(id = id)))

  override def validate(value: Json): ValidatedNel[ValidationError, Unit] = _schema.validate(value)

  def validateF[F[_]: Sync](value: Json): F[Either[NonEmptyList[ValidationError], Unit]] = Sync[F].delay(validate(value).toEither)
}
