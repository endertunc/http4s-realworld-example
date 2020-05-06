package com.real.world.http4s.authentication

import cats.effect.IO

import io.circe.refined._

import com.real.world.http4s.model.NewTypeImplicits._
import com.real.world.http4s.model.UserId

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import eu.timepit.refined.auto._
import tsec.jws.mac.JWTMac
import tsec.mac.jca.HMACSHA512

class TsecJWTSpec extends AsyncFlatSpec with Matchers {
  val tsecJJWT: TsecJWT[IO, HMACSHA512] = new TsecJWT[IO, HMACSHA512](HMACSHA512.generateKey[IO].unsafeRunSync)

  "TsecJWT" should "generate JWT" in {
    (for {
      jwt       <- tsecJJWT.generateJwt(UserId(1))
      parsedJwt <- JWTMac.parseUnverified[IO, HMACSHA512](jwt.value.value)
    } yield parsedJwt.body.getCustom[UserId]("userId") should be(Right(UserId(1)))).unsafeRunSync()
  }

  it should "verify JWT" in {
    (for {
      jwt          <- tsecJJWT.generateJwt(UserId(1))
      verifiedUser <- tsecJJWT.verify(jwt.value.value)
    } yield verifiedUser shouldBe UserId(1)).unsafeRunSync()
  }

}
