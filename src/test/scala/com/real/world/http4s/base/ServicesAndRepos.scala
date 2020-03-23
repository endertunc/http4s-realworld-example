package com.real.world.http4s.base

import scala.concurrent.Future

import org.http4s.syntax.string._
import org.http4s.{ AuthScheme, EntityBody }

import cats.effect.IO

import io.circe.Encoder
import io.circe.syntax._

import com.colisweb.tracing.context.OpenTracingContext
import com.colisweb.tracing.core.TracingContextBuilder

import io.opentracing.util.GlobalTracer
import io.opentracing.{ Span, Tracer }

import com.real.world.http4s.generators.{ ArticleGenerator, UserRegisterGenerator }
import com.real.world.http4s.model.article.Article
import com.real.world.http4s.model.tag.Tag
import com.real.world.http4s.model.user.User
import com.real.world.http4s.model.{ Pagination, UserId }
import com.real.world.http4s.module.RealWorldModule
import com.real.world.http4s.authentication.PasswordHasher

import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers

import eu.timepit.refined.api.Refined
import fs2.Stream
import fs2.text.utf8Encode
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.scalactic.source.Position
import tsec.mac.jca.{ HMACSHA512, MacSigningKey }

trait ServicesAndRepos extends IntegrationSpecBase with Matchers {

  implicit def refinedValue[T, P](refined: Refined[T, P]): T = refined.value

  implicit lazy val signingKey: MacSigningKey[HMACSHA512]            = HMACSHA512.generateKey[IO].unsafeRunSync
  implicit lazy val logger: SelfAwareStructuredLogger[IO]            = Slf4jLogger.create[IO].unsafeRunSync
  implicit lazy val tracingContextBuilder: TracingContextBuilder[IO] = OpenTracingContext.builder[IO, Tracer, Span](GlobalTracer.get()).unsafeRunSync
  implicit lazy val tsecPasswordHasher: PasswordHasher[IO]           = ctx.tsec

  val TokenAuthScheme: AuthScheme   = "Token".ci
  val defaultPagination: Pagination = Pagination(20, 0)
  val ctx                           = new RealWorldModule[IO]()

  def insertUser(): IO[User] = {
    val userRegister = UserRegisterGenerator.generateUserRegister
    ctx.userService.registerUser(userRegister)
  }

  def insertArticle(userId: UserId): IO[(Article, List[Tag])] = {
    val createArticle = ArticleGenerator.generateCreateArticleWrapper.article
    ctx.articleService.createArticle(createArticle, userId)
  }

  // ToDo move somewhere else
  def IOSuit[A](testBlock: => IO[Assertion])(implicit pos: Position): Future[Assertion] = testBlock.unsafeToFuture()

  // ToDo move somewhere else
  def FailedIOSuit[A](testCode: => IO[Assertion])(onError: Throwable => Assertion): Assertion =
    testCode
      .handleErrorWith(appError => IO(onError(appError)))
      .unsafeRunSync()

  def stringToEntityBody(body: String): EntityBody[IO] = Stream(body).through(utf8Encode)

  implicit class ToJsonEntityBody[T: Encoder](model: T) {
    def toJsonBody: EntityBody[IO] = stringToEntityBody(model.asJson.spaces2)
  }

}
