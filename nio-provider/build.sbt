import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

name := """nio-provider"""
organization := "fr.maif"

resolvers ++= Seq(
  "Maven central" at "http://repo1.maven.org/maven2/"
)

lazy val `nio-provider` = (project in file("."))
  .enablePlugins(PlayScala, DockerPlugin)
  .enablePlugins(NoPublish)
  .disablePlugins(BintrayPlugin)

scalaVersion := "2.12.4"

resolvers ++= Seq(
  Resolver.jcenterRepo
)

libraryDependencies ++= Seq(
  ws,
  "com.typesafe.play" %% "play-json" % "2.6.9",
  "com.typesafe.play" %% "play-json-joda" % "2.6.9",
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.22",
  "de.svenkubiak" % "jBCrypt" % "0.4.1", //  ISC/BSD
  "com.auth0" % "java-jwt" % "3.1.0", // MIT license
  "com.github.pureconfig" %% "pureconfig" % "0.9.1", // Apache 2.0
  "org.scalactic" %% "scalactic" % "3.0.4", // Apache 2.0
  "org.webjars" % "swagger-ui" % "3.12.1",
  "org.typelevel" %% "cats-core" % "1.1.0", // MIT
  "com.softwaremill.macwire" %% "macros" % "2.3.1" % "provided",
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
assemblyJarName in assembly := "nio-provider.jar"
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
packageName in Docker := "nio-provider"

maintainer in Docker := "MAIF Team <maif@maif.fr>"

dockerBaseImage := "openjdk:8"

dockerCommands ++= Seq(
  Cmd("ENV", "APP_NAME Nio-provider"),
  Cmd("ENV", "HTTP_PORT 9001"),
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

packageName in Universal := s"nio-provider"
