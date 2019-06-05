import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

name := """nio-server"""
organization := "fr.maif"

resolvers ++= Seq(
  "Maven central" at "http://repo1.maven.org/maven2/"
)

lazy val `nio-server` = (project in file("."))
  .enablePlugins(PlayScala, DockerPlugin)
  .enablePlugins(NoPublish)
  .disablePlugins(BintrayPlugin)

scalaVersion := "2.12.4"

resolvers ++= Seq(
  Resolver.jcenterRepo
)

libraryDependencies ++= Seq(
  ws,
  jdbc,
  "com.typesafe.play" %% "play-json" % "2.6.9",
  "com.typesafe.play" %% "play-json-joda" % "2.6.9",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.13.0-play26",
  "org.reactivemongo" %% "reactivemongo-akkastream" % "0.13.0",
  "org.postgresql" % "postgresql" % "42.2.5",
  "org.liquibase" % "liquibase-core" % "3.6.3",
  "org.scalikejdbc" %% "scalikejdbc-async" % "0.11.0",
  "org.scalikejdbc" %% "scalikejdbc-streams" % "3.3.2",
  "com.github.mauricio" %% "postgresql-async" % "0.2.21",
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.22",
  "de.svenkubiak" % "jBCrypt" % "0.4.1", //  ISC/BSD
  "com.auth0" % "java-jwt" % "3.1.0", // MIT license
  "com.github.pureconfig" %% "pureconfig" % "0.9.1", // Apache 2.0
  "org.scalactic" %% "scalactic" % "3.0.4", // Apache 2.0
  "org.webjars" % "swagger-ui" % "3.12.1",
  "org.typelevel" %% "cats-core" % "1.1.0", // MIT
  "com.softwaremill.macwire" %% "macros" % "2.3.1" % "provided",
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

/// ASSEMBLY CONFIG

mainClass in assembly := Some("play.core.server.ProdServerStart")
test in assembly := {}
assemblyJarName in assembly := "nio.jar"
fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)
assemblyMergeStrategy in assembly := {
  //case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case PathList("javax", xs @ _*) =>
    MergeStrategy.first
  case PathList("org", "apache", "commons", "logging", xs @ _*) =>
    MergeStrategy.discard
  case PathList(ps @ _*) if ps.last == "io.netty.versions.properties" =>
    MergeStrategy.first
  case PathList(ps @ _*) if ps.contains("reference-overrides.conf") =>
    MergeStrategy.concat
  case PathList(ps @ _*) if ps.last endsWith ".conf" => MergeStrategy.concat
  case PathList(ps @ _*) if ps.contains("buildinfo") =>
    MergeStrategy.discard
  case o =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(o)
}

lazy val packageAll = taskKey[Unit]("PackageAll")
packageAll := {
  (dist in Compile).value
  (assembly in Compile).value
}

/// DOCKER CONFIG

dockerExposedPorts := Seq(
  9000
)
packageName in Docker := "nio"

maintainer in Docker := "MAIF Team <maif@maif.fr>"

dockerBaseImage := "openjdk:8"

dockerCommands ++= Seq(
  Cmd("ENV", "APP_NAME Nio"),
  Cmd("ENV", "APP_VERSION 1.0.0-SNAPSHOT"),
  Cmd("ENV", "KAFKA_HOST kafka:29092"),
  Cmd("ENV", "CELLAR_ADDON_HOST http://s3server:8000"),
  Cmd("ENV", "MONGODB_ADDON_URI mongodb://mongo:27017/nio"),
  Cmd("ENV", "HTTP_PORT 9000"),
  Cmd("ENV", "APPLICATION_SECRET 123456")
)

dockerExposedVolumes ++= Seq(
  "/data"
)

dockerUsername := Some("maif")

//dockerRepository := Some("maif-docker-docker.bintray.io")

dockerCommands :=
  dockerCommands.value.flatMap {
    case ExecCmd("ENTRYPOINT", args @ _*) =>
      Seq(Cmd("ENTRYPOINT", args.mkString(" ")))
    case v => Seq(v)
  }

dockerEntrypoint ++= Seq(
  """-Dlogger.file=./conf/prod-logger.xml """,
  """-Dcluster.akka.remote.netty.tcp.hostname="$(eval "awk 'END{print $1}' /etc/hosts")" """,
  """-Dcluster.akka.remote.netty.tcp.bind-hostname="$(eval "awk 'END{print $1}' /etc/hosts")" """
)

dockerUpdateLatest := true

packageName in Universal := s"nio"
