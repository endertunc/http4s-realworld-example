package com.real.world.http4s

import tsec.mac.jca.HMACSHA512
import com.real.world.http4s.model.Pagination
import com.real.world.http4s.model.UserId
import org.scalactic.source.Position
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.Encoder
import com.real.world.http4s.model.user.User
import io.opentracing.util.GlobalTracer
import io.opentracing.Span
import eu.timepit.refined.api.Refined
import org.scalatest.matchers.should.Matchers
import fs2.text.utf8Encode
import com.colisweb.tracing.core.TracingContext

import scala.concurrent.Future
import com.real.world.http4s.model.tag.Tag
import com.real.world.http4s.module.RealWorldModule
import org.scalatest.Suite
import com.real.world.http4s.generators.ArticleGenerator
import com.real.world.http4s.model.article.Article
import com.real.world.http4s.generators.UserRegisterGenerator
import fs2.Stream
import com.colisweb.tracing.context.OpenTracingContext
import org.http4s.AuthScheme
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import cats.effect.IO
import com.real.world.http4s.authentication.PasswordHasher
import tsec.mac.jca.MacSigningKey
import com.colisweb.tracing.core.TracingContextBuilder
import io.opentracing.Tracer
import org.scalatest.Assertion
import org.http4s.EntityBody
import org.http4s.syntax.string._
import io.circe.syntax._

trait RealWorldApp extends EmbeddedPostgresSupport with Matchers { self: Suite =>

  implicit lazy val signingKey: MacSigningKey[HMACSHA512] = HMACSHA512.generateKey[IO].unsafeRunSync
  implicit lazy val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.create[IO].unsafeRunSync

  private val tracer: Tracer                                             = GlobalTracer.get() // It uses NoopTracer by default
  implicit lazy val noOpTracingContextBuilder: TracingContextBuilder[IO] = OpenTracingContext.builder[IO, Tracer, Span](tracer).unsafeRunSync
  implicit lazy val tsecPasswordHasher: PasswordHasher[IO]               = ctx.tsec
  implicit lazy val dummyTracingContext: TracingContext[IO]              = noOpTracingContextBuilder.build("dummyContext").use(IO(_)).unsafeRunSync()

  val TokenAuthScheme: AuthScheme   = "Token".ci
  val defaultPagination: Pagination = Pagination(20, 0) // scalastyle:off magic.number
  val ctx                           = new RealWorldModule[IO]()

  // ToDo upps I forgot to move these two function to quill packages.
  def insertUser(): IO[User] = {
    val userRegister = UserRegisterGenerator.generateUserRegisterRequest
    ctx.userService.registerUser(userRegister)
  }

  def insertArticle(userId: UserId): IO[(Article, List[Tag])] = {
    val createArticle = ArticleGenerator.generateCreateArticleRequest
    ctx.articleService.createArticle(createArticle, userId)
  }

  // ToDo create base test spec and add these two to there. Not everyone single test has to extend this trait.
  def IOSuit[A](testBlock: => IO[Assertion])(implicit pos: Position): Future[Assertion] = testBlock.unsafeToFuture()

  def FailedIOSuit[A](testCode: => IO[Assertion])(onError: Throwable => Assertion): Assertion =
    testCode.handleErrorWith(appError => IO(onError(appError))).unsafeRunSync()

  implicit def refinedValue[T, P](refined: Refined[T, P]): T = refined.value
  implicit class ToJsonEntityBody[T: Encoder](model: T) {
    def toJsonBody: EntityBody[IO] = stringToEntityBody(model.asJson.spaces2)
  }
  def stringToEntityBody(body: String): EntityBody[IO] = Stream(body).through(utf8Encode)
}
