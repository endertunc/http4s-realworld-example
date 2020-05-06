//package com.real.world.http4s.json
//
//import org.http4s._
//import org.http4s.circe._
//
//import cats.data.{ EitherT, NonEmptyList }
//import cats.effect.Sync
//import cats.implicits._
//
//import io.circe.jawn.parse
//import io.circe.schema.ValidationError
//import io.circe.{ Decoder, DecodingFailure }
//
//import com.real.world.http4s.model.ErrorWrapperOut
//import com.real.world.http4s.model.ErrorWrapperOut.JsonSchemaErrorToErrorWrapper
//
//trait CirceJsonEncoderWithSchemaValidator {
//
//  def jsonDecoderAdaptive[F[_]: Sync, A]()(implicit decoder: Decoder[A], schemaValidator: CirceSchemaValidatorWrapper[A]): EntityDecoder[F, A] =
//    EntityDecoder.decodeBy(MediaType.application.json) { msg =>
//      for {
//        raw   <- EitherT.liftF[F, DecodeFailure, String](EntityDecoder.decodeString(msg))
//        json  <- EitherT(Sync[F].delay(parse(raw))).leftMap[DecodeFailure](pe => MalformedMessageBodyFailure("Invalid JSON", Some(pe)))
//        _     <- EitherT(schemaValidator.validateF(json)).leftMap[DecodeFailure](ve => defaultCirceParseError(ve))
//        model <- EitherT(Sync[F].delay(decoder.decodeJson(json).leftMap(de => defaultCirceDecodingError(de))))
//      } yield model
//    }
//
//  private lazy val defaultCirceParseError: NonEmptyList[ValidationError] => DecodeFailure =
//    ve =>
//      new MessageBodyFailure {
//        override def toHttpResponse[F[_]](httpVersion: HttpVersion): Response[F] =
//          Response(Status.BadRequest, httpVersion)
//            .withEntity(ve.toErrorWrapper)(jsonEncoderOf[F, ErrorWrapperOut])
//
//        override def message: String          = s"Invalid message body: ${ve}"
//        override def cause: Option[Throwable] = None
//      }
//
//  private lazy val defaultCirceDecodingError: DecodingFailure => DecodeFailure =
//    df =>
//      new DecodeFailure {
//
//        override def message: String          = "The request body was malformed."
//        override def cause: Option[Throwable] = Some(df)
//
//        override def toHttpResponse[F[_]](httpVersion: HttpVersion): Response[F] =
//          Response(Status.BadRequest, httpVersion)
//            .withEntity(ErrorWrapperOut.fromString(message))(jsonEncoderOf[F, ErrorWrapperOut])
//      }
//
//}
