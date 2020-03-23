package com.real.world.http4s

import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder
import cats.effect._
import cats.implicits._
import doobie.Transactor
import doobie.util.ExecutionContexts
import com.colisweb.tracing.context.OpenTracingContext
import com.colisweb.tracing.core.TracingContextBuilder
import io.opentracing.util.GlobalTracer
import com.real.world.http4s.config.AppConfig.AppConfig
import com.real.world.http4s.db.migration.FlywayDatabaseMigrator
import com.real.world.http4s.module.RealWorldModule
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import tsec.mac.jca.{ HMACSHA512, MacSigningKey }

// $COVERAGE-OFF$
object Http4sRealWorldApp extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = new Http4sRealWorldApp[IO].server().use(_ => IO.never).as(ExitCode.Success)
}

class Http4sRealWorldApp[F[_]: ConcurrentEffect: ContextShift: Timer] {

  import pureconfig.generic.auto._
  def server(): Resource[F, Server[F]] =
    for {
      config                                                     <- Resource.liftF(ConfigSource.default.loadOrThrow[AppConfig].pure[F])
      txnEc                                                      <- ExecutionContexts.cachedThreadPool[F]
      connEc                                                     <- ExecutionContexts.fixedThreadPool[F](config.databaseConfig.connections.poolSize)
      databaseMigrator                                           <- Resource.liftF(FlywayDatabaseMigrator[F].pure[F])
      implicit0(xa: Transactor[F])                               <- databaseMigrator.dbTransactor(config.databaseConfig, connEc, Blocker.liftExecutionContext(txnEc))
      implicit0(loggerF: SelfAwareStructuredLogger[F])           <- Resource.liftF(Slf4jLogger.create[F])
      implicit0(signingKey: MacSigningKey[HMACSHA512])           <- Resource.liftF(HMACSHA512.generateKey[F])
      implicit0(tracingContextBuilder: TracingContextBuilder[F]) <- Resource.liftF(OpenTracingContext.builder(GlobalTracer.get()))
      ctx                                                        <- Resource.liftF(new RealWorldModule[F]().pure[F])
      _                                                          <- Resource.liftF(databaseMigrator.migrate(xa))
      server <- BlazeServerBuilder[F]
        .bindHttp(config.http.port, config.http.host)
        .withHttpApp(ctx.httpApp)
        .resource
    } yield server

}
