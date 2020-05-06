package com.real.world.http4s.module

import scala.concurrent.duration._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.middleware.{ CORSConfig, CORS, Logger => RequestResponseLogger }
import org.http4s.{ HttpApp, HttpRoutes, Response }
import cats.data.Kleisli._
import cats.data.{ Kleisli, OptionT }
import cats.effect.{ ConcurrentEffect, ContextShift }
import cats.implicits._
import com.colisweb.tracing.core.TracingContext
import doobie.util.transactor.Transactor
import com.colisweb.tracing.core.TracingContextBuilder
import com.real.world.http4s.authentication.{ JwtAuthenticator, PasswordHasher, SCryptPasswordHasher, TsecJWT }
import com.real.world.http4s.http.BaseHttpErrorHandler
import com.real.world.http4s.http.middleware.{ AuthUserMiddleware, TracedContextMiddleware }
import com.real.world.http4s.http.routes._
import com.real.world.http4s.model.UserId
import com.real.world.http4s.model.tag.TagResponse
import com.real.world.http4s.repository._
import com.real.world.http4s.repository.algebra._
import com.real.world.http4s.service.{ ArticleService, CommentService, FollowerService, ProfileService, TagService, UserService }
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import org.http4s.ContextRequest
import org.http4s.ContextRoutes
import org.http4s.HttpRoutes
import org.http4s.server.ContextMiddleware
import tsec.mac.jca.{ HMACSHA512, MacSigningKey }
import tsec.passwordhashers.jca.SCrypt

// $COVERAGE-OFF$

class RealWorldModule[F[_]: ConcurrentEffect: ContextShift: Transactor: SelfAwareStructuredLogger: TracingContextBuilder]()(
    implicit signingKey: MacSigningKey[HMACSHA512]
) extends Http4sDsl[F] {

  implicit val tsec: PasswordHasher[F]               = new SCryptPasswordHasher[F](SCrypt)
  implicit val jwtAuthenticator: JwtAuthenticator[F] = new TsecJWT[F, HMACSHA512](signingKey)

  implicit val userRepository: UserRepositoryAlgebra[F]          = PostgresUserRepositoryAlgebra[F]()
  implicit val articleRepository: ArticleRepositoryAlgebra[F]    = PostgresArticleRepositoryAlgebra[F]()
  implicit val tagRepository: TagRepositoryAlgebra[F]            = PostgresTagRepositoryAlgebra[F]()
  implicit val followersRepository: FollowerRepositoryAlgebra[F] = PostgresFollowerRepositoryAlgebra[F]()
  implicit val commentsRepository: CommentRepositoryAlgebra[F]   = PostgresCommentRepositoryAlgebra[F]()

  implicit val userService: UserService[F]         = UserService[F]()
  implicit val followerService: FollowerService[F] = FollowerService[F]()
  implicit val profileService: ProfileService[F]   = ProfileService[F]()
  implicit val tagService: TagService[F]           = TagService[F]()
  implicit val articleService: ArticleService[F]   = ArticleService[F]()
  implicit val commentService: CommentService[F]   = CommentService[F]()

  private val authenticationRoute: AuthenticationHttpRoutes[F] = AuthenticationHttpRoutes[F]()
  private val userRoute: UserRoutes[F]                         = UserRoutes[F]()
  private val tagRoute: TagRoutes[F]                           = TagRoutes[F]()
  private val profileRoute: ProfileRoutes[F]                   = ProfileRoutes[F]()
  private val articleRoute: ArticleRoutes[F]                   = ArticleRoutes[F]()
  private val errorHandler                                     = new BaseHttpErrorHandler[F]()

  implicit val authUserMiddleware: AuthUserMiddleware[F] = new AuthUserMiddleware[F]()

  def httpApp: HttpApp[F] =
    errorHandler
      .handle(
        CORS(
          endpoints,
          CORSConfig(anyOrigin = true, anyMethod = true, allowCredentials = true, maxAge = 1.day.toSeconds)
        )
      )
      .orNotFound

  def endpoints: HttpRoutes[F] =
    // You can introduce a flag to toggle these settings -
    // Just make sure you don't log anything sensitive on live/prod
    RequestResponseLogger.httpRoutes(logHeaders = true, logBody = true)(
      Router(
        "/api" -> Router(
          "/users" -> authenticationRoute.routes,
          "/tags" -> tagRoute.routes,
          "/articles" -> {
            authUserMiddleware.optionalAuthMiddleware(TracedContextMiddleware(articleRoute.optionallySecuredRoutes)) <+>
            authUserMiddleware.authMiddleware(TracedContextMiddleware(articleRoute.securedRoutes))
          },
          "/user" -> authUserMiddleware.authMiddleware(TracedContextMiddleware(userRoute.routes)),
          "/profiles" -> authUserMiddleware.authMiddleware(TracedContextMiddleware(profileRoute.routes))
        )
      )
    ) <+>
    // This needs to be here all the time to log NotFound requests in any case
    RequestResponseLogger.httpRoutes(logHeaders = true, logBody = false)(Kleisli(_ => OptionT.pure(Response.notFound)))
}
// $COVERAGE-ON$
