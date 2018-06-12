name := """nio-server"""
organization := "fr.maif"

resolvers ++= Seq(
  "Maven central" at "http://repo1.maven.org/maven2/"
)

lazy val `nio-server` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  guice,
  ws,
  "com.typesafe.play" %% "play-json" % "2.6.9",
  "com.typesafe.play" %% "play-json-joda" % "2.6.9",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.13.0-play26",
  "org.reactivemongo" %% "reactivemongo-akkastream" % "0.13.0",
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.20",
  "de.svenkubiak" % "jBCrypt" % "0.4.1", //  ISC/BSD
  "com.auth0" % "java-jwt" % "3.1.0", // MIT license
  "com.github.pureconfig" %% "pureconfig" % "0.8.0", // Apache 2.0
  "org.scalactic" %% "scalactic" % "3.0.4", // Apache 2.0
  "org.webjars" % "swagger-ui" % "3.12.1",
  "org.typelevel" %% "cats-core" % "1.1.0", // MIT
  // https://mvnrepository.com/artifact/com.amazonaws/aws-java-sdk-s3
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.224", // Apache 2.0
  "io.dropwizard.metrics" % "metrics-core" % "4.0.2", // Apache 2.0
  "io.dropwizard.metrics" % "metrics-json" % "4.0.2", // Apache 2.0
  "io.dropwizard.metrics" % "metrics-jvm" % "4.0.2", // Apache 2.0

  // S3 client for akka-stream
  "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "0.14",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.0" % Test
)

scalacOptions ++= Seq(
  "-feature",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:existentials"
)

scalafmtOnCompile in ThisBuild := true

scalafmtTestOnCompile in ThisBuild := true

scalafmtVersion in ThisBuild := "1.2.0"
