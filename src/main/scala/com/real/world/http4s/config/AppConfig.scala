package com.real.world.http4s.config

// ToDo use pureconfig with refined
object AppConfig {
  case class AppConfig(http: Http, databaseConfig: DatabaseConfig)

  case class Http(port: Int, host: String)

  case class DatabaseConnectionsConfig(poolSize: Int)
  case class DatabaseConfig(
      driver: String,
      url: String,
      user: String,
      password: String,
      connections: DatabaseConnectionsConfig
  )
}
