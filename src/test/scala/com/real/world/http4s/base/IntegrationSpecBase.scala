package com.real.world.http4s.base

import cats.effect.{ Blocker, ContextShift, IO }

import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor

trait IntegrationSpecBase {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContexts.synchronous)

  implicit lazy val xa: doobie.Transactor[IO] =
    Transactor.fromDriverManager[IO](
      "org.postgresql.Driver", // driver classname
      url  = PostgresqlTestContainer.container.jdbcUrl.concat("&TC_REUSABLE=false"), // connect URL (driver-specific)
      user = PostgresqlTestContainer.dbUsername, // user
      pass = PostgresqlTestContainer.dbPass, // password
      Blocker.liftExecutionContext(ExecutionContexts.synchronous) // just for testing
    )
}
