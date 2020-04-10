package com.real.world.http4s.db.migration

import scala.concurrent.ExecutionContext
import cats.effect.{ Async, Blocker, ContextShift, Resource, Sync }
import doobie.hikari.HikariTransactor
import com.real.world.http4s.config.AppConfig.DatabaseConfig
import org.flywaydb.core.Flyway

// ToDo everything needs to behind a Trait!
// $COVERAGE-OFF$
final class FlywayDatabaseMigrator[F[_]: Async: ContextShift] {

  def dbTransactor(dbc: DatabaseConfig, connEc: ExecutionContext, blocker: Blocker): Resource[F, HikariTransactor[F]] =
    HikariTransactor
      .newHikariTransactor[F](
        driverClassName = dbc.driver,
        url             = dbc.url,
        user            = dbc.user,
        pass            = dbc.password,
        connectEC       = connEc,
        blocker         = blocker
      )

  def migrate(transactor: HikariTransactor[F]): F[Int] =
    transactor.configure(dataSource => Sync[F].delay(thunk = Flyway.configure().dataSource(dataSource).load().migrate()))

}

object FlywayDatabaseMigrator {
  def apply[F[_]: Async: ContextShift]: FlywayDatabaseMigrator[F] =
    new FlywayDatabaseMigrator()
}

// $COVERAGE-ON$
