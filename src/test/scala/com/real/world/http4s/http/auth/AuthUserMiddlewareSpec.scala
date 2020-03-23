package com.real.world.http4s.http.auth

import org.http4s.Credentials.Token
import org.http4s.headers.Authorization
import org.http4s.{ Status, _ }
import cats.effect.IO
import com.real.world.http4s.AppError.{ InvalidAuthorizationHeader, MissingAuthorizationHeader }
import com.real.world.http4s.base.ServicesAndRepos
import com.real.world.http4s.model.UserId
import com.real.world.http4s.authentication.JwtAuthenticator
import com.real.world.http4s.http.middleware.AuthUserMiddleware
import com.real.world.http4s.{ model, AppError }
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.{ EitherValues, OptionValues }
import eu.timepit.refined.auto._

class AuthUserMiddlewareSpec extends AsyncFlatSpec with ServicesAndRepos with OptionValues with EitherValues {

  implicit val dummyJwtAuthenticator: JwtAuthenticator[IO] = new JwtAuthenticator[IO] {
    override def generateJwt(userId: UserId): IO[model.Token] = IO.delay(model.Token("FakeToken"))
    override def verify(jwt: String): IO[UserId]              = IO(userId)
  }
  private val userId             = UserId(1)
  private val authUserMiddleware = new AuthUserMiddleware[IO]()

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
