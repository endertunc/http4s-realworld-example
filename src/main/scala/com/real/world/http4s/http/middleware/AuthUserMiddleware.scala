package com.real.world.http4s.http.middleware

import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware
import org.http4s.syntax.string._

import cats.data.{ EitherT, Kleisli, OptionT }
import cats.effect.Sync
import cats.implicits._

import com.real.world.http4s.AppError
import com.real.world.http4s.AppError.{ InvalidAuthorizationHeader, MissingAuthorizationHeader }
import com.real.world.http4s.authentication.JwtAuthenticator
import com.real.world.http4s.model._

import io.chrisdavenport.log4cats.Logger
import mouse.all._

class AuthUserMiddleware[F[_]: Sync: Logger]()(implicit jwtAuthenticator: JwtAuthenticator[F]) extends Http4sDsl[F] {

  val Token: AuthScheme = "Token".ci

  def authUser: Kleisli[F, Request[F], Either[AppError, UserId]] =
    Kleisli { request =>
      (for {
        authorizationHeader <- request.headers
          .get(Authorization)
          .toRight[AppError](MissingAuthorizationHeader("Couldn't find an Authorization header"))
          .toEitherT[F]
        jwt    <- parseToken(authorizationHeader) |> (EitherT(_))
        userId <- jwtAuthenticator.verify(jwt) |> (EitherT.right[AppError](_))
      } yield userId).value
    }

  def optionalAuthUser: Kleisli[F, Request[F], Either[AppError, Option[UserId]]] = authUser.map {
    case Right(userId)                       => Some(userId).asRight[AppError]
    case Left(_: MissingAuthorizationHeader) => None.asRight[AppError]
    case Left(appError)                      => appError.asLeft[Option[UserId]]
  }

  private def parseToken(authorizationHeader: Authorization): F[Either[AppError, String]] =
    authorizationHeader.credentials match {
      case Credentials.Token(Token, token) =>
        token.asRight[AppError].pure[F]
      case credentials: Credentials =>
        Logger[F].warn(s"Unsupported auth schema: [${credentials.authScheme}]") *>
        (InvalidAuthorizationHeader("Expected schema was Bearer"): AppError).asLeft[String].pure[F]
    }

  val onFailure: AuthedRoutes[AppError, F] = Kleisli { req =>
    OptionT.liftF(Logger[F].warn(req.context)("Failed authentication attempt") *> req.context.toHttpResponse())
  }

  val authMiddleware: AuthMiddleware[F, UserId]                 = AuthMiddleware(authUser, onFailure)
  val optionalAuthMiddleware: AuthMiddleware[F, Option[UserId]] = AuthMiddleware(optionalAuthUser, onFailure)

}
