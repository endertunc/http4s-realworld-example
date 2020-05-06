package com.real.world.http4s.authentication

import scala.concurrent.duration._

import cats.effect.Sync
import cats.implicits._

import io.circe.refined._
import io.circe.syntax._
import io.circe.syntax._

import com.real.world.http4s.AppError
import com.real.world.http4s.model.NewTypeImplicits._
import com.real.world.http4s.model._

import tsec.jws.mac.{ JWSMacCV, JWTMac }
import tsec.jwt.JWTClaims
import tsec.jwt.algorithms.JWTMacAlgo
import tsec.mac.jca.MacSigningKey

class TsecJWT[F[_]: Sync, A: JWTMacAlgo](signingKey: MacSigningKey[A])(implicit s: JWSMacCV[F, A]) extends JwtAuthenticator[F] {

  override def generateJwt(userId: UserId): F[Token] =
    for {
      claims <- JWTClaims.withDuration[F](customFields = Seq("userId" -> userId.asJson), expiration = Some(10.minutes))
      jwt    <- JWTMac.buildToString[F, A](claims, signingKey)
    } yield Token.unsafeFrom(jwt)

  override def verify(jwt: String): F[UserId] =
    for {
      parsed <- JWTMac.verifyAndParse[F, A](jwt, signingKey)
      userId <- parsed.body.getCustom[UserId]("userId").leftMap(_ => AppError.JwtUserIdMalformed("Malformed JWT")).liftTo[F]
    } yield userId

}
