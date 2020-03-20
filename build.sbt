lazy val CatsEffectVersion               = "2.0.0"
lazy val Fs2Version                      = "2.1.0"
lazy val Http4sVersion                   = "0.21.0-M6"
lazy val CirceVersion                    = "0.12.3"
lazy val CirceGenericExtrasVersion       = "0.12.2"
lazy val CirceJsonSchemaVersion          = "0.1.0"
lazy val ScalaJsonSchemaCirceJsonVersion = "0.2.2"
lazy val DoobieVersion                   = "0.8.8"
lazy val H2Version                       = "1.4.200"
lazy val FlywayVersion                   = "6.1.3"
lazy val LogbackVersion                  = "1.2.3"
lazy val ScalaTestVersion                = "3.1.0"
lazy val ScalaCheckVersion               = "1.14.3"
lazy val JsonSchemaVersion               = "0.2.2"
lazy val PureConfigVersion               = "0.12.1"
lazy val TsecVersion                     = "0.2.0-M2"
lazy val TestContainersScalaVersion      = "0.34.2"
lazy val Log4CatsVersion                 = "1.0.1"
lazy val BetterMonadicForVersion         = "0.3.1"
lazy val RandomDataGeneratorVersion      = "2.8"

lazy val root = (project in file("."))
  .settings(scalastyleSettings ++ testSettings)
  .settings(
    organization := "com.realworld",
    name := "http4s-realworld-example",
    version := "1.0.0-SNAPSHOT",
    scalaVersion := "2.12.10",
    resolvers += "jitpack".at(location = "https://jitpack.io"), // required for Everit dependencies which is used by circe-json-schema
    addCompilerPlugin(scalafixSemanticdb),
    scalacOptions := Seq(
        "-unchecked",
        "-deprecation",
        "-encoding",
        "UTF-8",
        "-feature",
        "-language:existentials",
        "-language:higherKinds",
        "-Ypartial-unification",
        "-language:postfixOps",
        "-language:implicitConversions",
        "-Yrangepos",
        "-Ywarn-unused-import"
      ),
    libraryDependencies ++= Seq(
        "org.typelevel"         %% "cats-effect"                     % CatsEffectVersion,
        "co.fs2"                %% "fs2-core"                        % Fs2Version,
        "org.http4s"            %% "http4s-blaze-server"             % Http4sVersion,
        "org.http4s"            %% "http4s-circe"                    % Http4sVersion,
        "org.http4s"            %% "http4s-dsl"                      % Http4sVersion,
        "io.circe"              %% "circe-core"                      % CirceVersion,
        "io.circe"              %% "circe-generic"                   % CirceVersion,
        "io.circe"              %% "circe-parser"                    % CirceVersion,
        "io.circe"              %% "circe-literal"                   % CirceVersion,
        "io.circe"              %% "circe-shapes"                    % CirceVersion,
        "io.circe"              %% "circe-generic-extras"            % CirceGenericExtrasVersion,
        "com.h2database"        % "h2"                               % H2Version,
        "com.github.pureconfig" %% "pureconfig"                      % PureConfigVersion,
        "org.flywaydb"          % "flyway-core"                      % FlywayVersion,
        "org.tpolecat"          %% "doobie-core"                     % DoobieVersion,
        "org.tpolecat"          %% "doobie-postgres"                 % DoobieVersion,
        "org.tpolecat"          %% "doobie-h2"                       % DoobieVersion,
        "org.tpolecat"          %% "doobie-hikari"                   % DoobieVersion,
        "ch.qos.logback"        % "logback-classic"                  % LogbackVersion,
        "com.github.andyglow"   %% "scala-jsonschema-core"           % JsonSchemaVersion,
        "org.scalatest"         %% "scalatest"                       % ScalaTestVersion % Test,
        "org.scalacheck"        %% "scalacheck"                      % ScalaCheckVersion % Test,
        "org.tpolecat"          %% "doobie-scalatest"                % DoobieVersion % Test,
        "com.dimafeng"          %% "testcontainers-scala-scalatest"  % TestContainersScalaVersion % Test,
        "com.dimafeng"          %% "testcontainers-scala-postgresql" % TestContainersScalaVersion % Test,
        "io.github.jmcardon"    %% "tsec-http4s"                     % TsecVersion,
        "org.tpolecat"          %% "doobie-quill"                    % DoobieVersion,
        "io.chrisdavenport"     %% "log4cats-slf4j"                  % Log4CatsVersion,
        "io.circe"              %% "circe-json-schema"               % CirceJsonSchemaVersion,
        "com.github.andyglow"   %% "scala-jsonschema-circe-json"     % ScalaJsonSchemaCirceJsonVersion,
        "com.codecommit"        %% "cats-effect-testing-scalatest"   % "0.4.0",
        compilerPlugin("org.typelevel" %% "kind-projector"     % "0.11.0" cross CrossVersion.full),
        compilerPlugin("com.olegpy"    %% "better-monadic-for" % BetterMonadicForVersion),
        "org.typelevel" %% "mouse" % "0.24"
        //        "eu.timepit"            %% "refined"                         % "0.9.12",
      )
  )

// -----------------------------------------------------------------------------
// scalastyle settings
// -----------------------------------------------------------------------------

lazy val scalastyleSettings = Seq(
  scalastyleFailOnWarning := true
)

// -----------------------------------------------------------------------------
// scalastyle settings
// -----------------------------------------------------------------------------

lazy val testSettings = Seq(
  Test / parallelExecution := true,
  Test / concurrentRestrictions := Seq(
      Tags.limit(Tags.Test, max = 4) // scalastyle:ignore
    ),
  Test / logBuffered := false,
  Test / fork := true,
  Test / testForkedParallel := true,
  Test / testOptions += Tests.Argument("-oDS")
)

// -----------------------------------------------------------------------------
// other settings
// -----------------------------------------------------------------------------

Global / onChangedBuildSource := ReloadOnSourceChanges

addCommandAlias("qa", "; clean; compile; coverage; test; coverageReport; coverageAggregate")
