package com.real.world.http4s.base

import com.dimafeng.testcontainers.PostgreSQLContainer
import org.flywaydb.core.Flyway

object PostgresqlTestContainer {

  lazy val database   = "realworld"
  lazy val dbUsername = "realworld"
  lazy val dbPass     = "realworld"

  val container: PostgreSQLContainer = PostgreSQLContainer
    .Def(
      databaseName             = database,
      username                 = dbUsername,
      password                 = dbPass,
      mountPostgresDataToTmpfs = false
    )
    .start()

  val flyWay: Flyway = Flyway.configure().dataSource(container.jdbcUrl, dbUsername, dbPass).load()
  flyWay.migrate()

}
