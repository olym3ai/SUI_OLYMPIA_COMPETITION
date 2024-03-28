ThisBuild / scalaVersion     := "3.3.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"
val PekkoVersion = "1.0.2"
val CirceVersion = "0.14.6"

lazy val root = (project in file("."))
  .settings(
    name := "game-server",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % CirceVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "org.apache.pekko" %% "pekko-actor-typed" % PekkoVersion,
      "org.apache.pekko" %% "pekko-stream-typed" % PekkoVersion,
      "org.apache.pekko" %% "pekko-http"% "1.0.1",
      "ch.qos.logback" % "logback-classic" % "1.4.5",
      "ch.qos.logback" % "logback-core" % "1.4.5",
      "org.apache.pekko" %% "pekko-actor-testkit-typed" % PekkoVersion % Test,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
