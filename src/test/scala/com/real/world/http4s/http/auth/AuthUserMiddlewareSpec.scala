package com.real.world.http4s.http.auth

import com.real.world.http4s.AppError.{ InvalidAuthorizationHeader, MissingAuthorizationHeader }
import com.real.world.http4s.model.user.User
import com.real.world.http4s.model.user.User.UserId
import com.real.world.http4s.security.JwtAuthenticator
import com.real.world.http4s.AppError
import com.real.world.http4s.base.ServicesAndRepos
import com.real.world.http4s.AppError
import com.real.world.http4s.base.ServicesAndRepos
import com.real.world.http4s.model.user.User
import com.real.world.http4s.security.JwtAuthenticator
import org.scalatest.{ EitherValues, OptionValues }
import org.scalatest.flatspec.AsyncFlatSpec

import cats.effect.IO

import org.http4s.Credentials.Token
import org.http4s.headers.Authorization
import org.http4s.{ Status, _ }

class AuthUserMiddlewareSpec extends AsyncFlatSpec with ServicesAndRepos with OptionValues with EitherValues {
  private val userId = UserId(1)

  implicit val dummyJwtAuthenticator: JwtAuthenticator[IO] = new JwtAuthenticator[IO] {
    override def generateJwt(userId: User.UserId): IO[String] = IO.delay("FakeToken")
    override def verify(jwt: String): IO[UserId]              = IO(userId)
  }
  val authUserMiddleware = new AuthUserMiddleware[IO]()

  "AuthUserMiddleware" should "fail with MissingAuthorizationHeader when Authorization header is missing" in IOSuit {
    for {
      userOrError <- authUserMiddleware.authUser.run(Request[IO]())
    } yield userOrError should be(Left(MissingAuthorizationHeader("Couldn't find an Authorization header")))
  }

  it should "fail with InvalidAuthorizationHeader when schema is BasicCredentials" in IOSuit {
    for {
      userOrError <- authUserMiddleware.authUser.run(Request[IO]().withHeaders(Authorization(BasicCredentials("username", "password"))))
    } yield userOrError should be(Left(InvalidAuthorizationHeader("Expected schema was Bearer")))
  }

  it should "it should provide extracted userId" in IOSuit {
    val request = Request[IO](headers = Headers.of(Authorization(Token(TokenAuthScheme, "Token"))))
    for {
      userIdFromRequest <- authUserMiddleware.authUser.run(request)
    } yield userIdFromRequest should be(Right(userId))
  }

  "OptionalAuthUserMiddleware" should "return none when Authorization header missing" in IOSuit {
    for {
      userIdOrError <- authUserMiddleware.optionalAuthUser.run(Request[IO]())
    } yield userIdOrError.right.value should not be defined
  }

  it should "fail with InvalidAuthorizationHeader when schema is BasicCredentials" in IOSuit {
    val request = Request[IO]().withHeaders(Authorization(BasicCredentials("username", "password")))
    for {
      userOrError <- authUserMiddleware.optionalAuthUser.run(request)
    } yield userOrError should be(Left(InvalidAuthorizationHeader("Expected schema was Bearer")))
  }

  it should "provide extracted userId" in IOSuit {
    val request = Request[IO](headers = Headers.of(Authorization(Token(TokenAuthScheme, "Token"))))
    for {
      userIdAsOpt <- authUserMiddleware.optionalAuthUser.run(request)
    } yield userIdAsOpt.right.value should be(Some(userId))
  }

  "On failure" should "map AppError to response" in IOSuit {
    val authedRequest: AuthedRequest[IO, AppError] = AuthedRequest(InvalidAuthorizationHeader("OnFailure Test AppError"), Request[IO]())
    for {
      response <- authUserMiddleware.onFailure.run(authedRequest).value
    } yield {
      response.value.status shouldBe Status.BadRequest
      response.value.bodyAsText.compile.string.unsafeRunSync should include("OnFailure Test AppError")
    }
  }
}
