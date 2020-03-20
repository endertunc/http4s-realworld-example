package com.real.world.http4s.http

import com.real.world.http4s.json.{ CirceJsonEncoderWithSchemaValidator, CirceSchemaValidatorWrapper }
import com.real.world.http4s.AppError
import com.real.world.http4s.json.{ CirceJsonEncoderWithSchemaValidator, CirceSchemaValidatorWrapper }

import io.circe.{ Decoder, Encoder }
import io.circe.syntax._

import cats.effect.Async
import cats.implicits._

import org.http4s.{ EntityDecoder, Response }
import org.http4s.circe.CirceEntityEncoder
import org.http4s.dsl.Http4sDsl
import io.chrisdavenport.log4cats.Logger

abstract class BaseHttp4s[F[_]: Async: Logger] extends Http4sDsl[F] with CirceJsonEncoderWithSchemaValidator with CirceEntityEncoder {

  implicit def circeEntityDecoder[A: Decoder: CirceSchemaValidatorWrapper]: EntityDecoder[F, A] = jsonDecoderAdaptive[F, A]

  implicit def toResponse[A](result: F[A])(implicit encoder: Encoder[A]): F[Response[F]] =
    result.toResponse

  implicit class FToResponse[A](result: F[A]) {
    def toResponse(implicit encoder: Encoder[A]): F[Response[F]] =
      result
        .flatMap {
          case _: Unit => Ok()
          case model   => Ok(model.asJson)
        }
        .recoverWith {
          case appError: AppError => appError.toHttpResponse()
          case t: Exception       => Logger[F].error(t)(message = "Unexpected exception") *> InternalServerError("Internal Server Error")
        }
  }

}
