package com.real.world.http4s.base

import com.real.world.http4s.generators.{ ArticleGenerator, UserRegisterGenerator }
import com.real.world.http4s.model.article.Article
import com.real.world.http4s.model.tag.Tag
import com.real.world.http4s.model.user.User
import com.real.world.http4s.model.user.User.UserId
import com.real.world.http4s.model.Pagination
import com.real.world.http4s.module.RealWorldModule
import com.real.world.http4s.security.PasswordHasher
import com.real.world.http4s.generators.{ ArticleGenerator, UserRegisterGenerator }
import com.real.world.http4s.model.Pagination
import com.real.world.http4s.model.article.Article
import com.real.world.http4s.model.tag.Tag
import com.real.world.http4s.model.user.User
import com.real.world.http4s.module.RealWorldModule
import com.real.world.http4s.security.PasswordHasher

import scala.concurrent.Future

import io.circe.Encoder
import io.circe.syntax._

import cats.effect.IO

import org.http4s.{ AuthScheme, EntityBody }
import fs2.text.utf8Encode
import fs2.Stream
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.scalactic.source.Position
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import tsec.mac.jca.{ HMACSHA512, MacSigningKey }

import org.http4s.syntax.string._

trait ServicesAndRepos extends IntegrationSpecBase with Matchers {

  implicit lazy val signingKey: MacSigningKey[HMACSHA512]  = HMACSHA512.generateKey[IO].unsafeRunSync
  implicit lazy val logger: SelfAwareStructuredLogger[IO]  = Slf4jLogger.create[IO].unsafeRunSync()
  implicit lazy val tsecPasswordHasher: PasswordHasher[IO] = ctx.tsec

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
