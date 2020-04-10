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
import doobie.util.transactor.Transactor
import com.colisweb.tracing.core.TracingContextBuilder
import com.real.world.http4s.http.middleware.{ AuthUserMiddleware, AuthedTracedMiddleware }
import com.real.world.http4s.http.routes._
import com.real.world.http4s.repository._
import com.real.world.http4s.repository.algebra._
import com.real.world.http4s.authentication.{ JwtAuthenticator, PasswordHasher, SCryptPasswordHasher, TsecJWT }
import com.real.world.http4s.http.BaseHttpErrorHandler
import com.real.world.http4s.service.{ ArticleService, CommentService, FollowerService, ProfileService, TagService, UserService }
import io.chrisdavenport.log4cats.Logger
import tsec.mac.jca.{ HMACSHA512, MacSigningKey }
import tsec.passwordhashers.jca.SCrypt

// $COVERAGE-OFF$

class RealWorldModule[F[_]: ConcurrentEffect: ContextShift: Transactor: Logger: TracingContextBuilder]()(
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

  val authenticationRoute: AuthenticationHttpRoutes[F] = AuthenticationHttpRoutes[F]()
  val userRoute: UserRoutes[F]                         = UserRoutes[F]()
  val tagRoute: TagRoutes[F]                           = TagRoutes[F]()
  val profileRoute: ProfileRoutes[F]                   = ProfileRoutes[F]()
  val articleRoute: ArticleRoutes[F]                   = ArticleRoutes[F]()
  val anan: ExampleAuthedTracedRoutes[F]               = ExampleAuthedTracedRoutes[F]()
  val errorHandler                                     = new BaseHttpErrorHandler[F]()

  implicit val authUserMiddleware: AuthUserMiddleware[F] = new AuthUserMiddleware[F]()

//  def endpoints: HttpRoutes[F] =
//    // You can introduce a flag to toggle these settings -
//    // Just make sure you don't log anything sensitive on live/prod
//    RequestResponseLogger.httpRoutes(logHeaders = true, logBody = true)(
//      Router(
//        "/api" -> Router(
//          "/users" -> authenticationRoute.routes,
//          "/tags" -> tagRoute.routes,
//          "/articles" -> {
//            authUserMiddleware.optionalAuthMiddleware(articleRoute.optionallySecuredRoutes) <+>
//            authUserMiddleware.authMiddleware(articleRoute.securedRoutes)
//          },
//          "/user" -> authUserMiddleware.authMiddleware(userRoute.routes),
//          "/profiles" -> authUserMiddleware.authMiddleware(profileRoute.routes)
//        )
//      )
//    ) <+>
//    // This needs to be here all the time to log NotFound requests in any case
//    RequestResponseLogger.httpRoutes(logHeaders = true, logBody = false)(Kleisli(_ => OptionT.pure(Response.notFound)))

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
            authUserMiddleware.optionalAuthMiddleware(articleRoute.optionallySecuredRoutes) <+>
            authUserMiddleware.authMiddleware(AuthedTracedMiddleware(articleRoute.securedRoutes))
          },
          "/anan" -> authUserMiddleware.authMiddleware(AuthedTracedMiddleware(anan.routes)),
          "/user" -> authUserMiddleware.authMiddleware(AuthedTracedMiddleware(userRoute.routes)),
          "/profiles" -> authUserMiddleware.authMiddleware(AuthedTracedMiddleware(profileRoute.routes))
        )
      )
    ) <+>
    // This needs to be here all the time to log NotFound requests in any case
    RequestResponseLogger.httpRoutes(logHeaders = true, logBody = false)(Kleisli(_ => OptionT.pure(Response.notFound)))
}
// $COVERAGE-ON$
