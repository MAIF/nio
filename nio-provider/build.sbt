import Dependencies._
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

name := """nio-provider"""
organization := "fr.maif"

lazy val `nio-provider` = (project in file("."))
  .enablePlugins(PlayScala, DockerPlugin)
  .enablePlugins(NoPublish)
  .disablePlugins(BintrayPlugin)

scalaVersion := _scalaVersion

resolvers ++= Seq(
  Resolver.jcenterRepo,
  "Maven central" at "https://repo1.maven.org/maven2/"
)

dependencyOverrides ++= Seq(
  "com.github.luben"          % "zstd-jni"                 % "1.5.6-4"
)

libraryDependencies ++= Seq(
  ws,
  "org.apache.pekko"         %% "pekko-connectors-kafka"        % pekkoKafka,
  "de.svenkubiak"             % "jBCrypt"            % "0.4.1", //  ISC/BSD
  "com.auth0"                 % "java-jwt"           % javaJwt, // MIT license
//  "com.github.pureconfig"    %% "pureconfig"         % pureConfig, // Apache 2.0
  "com.github.pureconfig"    %% "pureconfig-core"          % pureConfig, // Apache 2.0
  "com.github.pureconfig"    %% "pureconfig-generic-scala3" % pureConfig, // Apache 2.0
  "org.scalactic"            %% "scalactic"          % scalaticVersion, // Apache 2.0
  "org.webjars"               % "swagger-ui"         % "3.12.1",
  "org.typelevel"            %% "cats-core"          % catsVersion, // MIT
  "com.softwaremill.macwire" %% "macros"             % macwireVersion % "provided",
  "org.scalatestplus.play"   %% "scalatestplus-play" % scalatestPlay  % Test
)

scalacOptions ++= Seq(
  "-feature",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:existentials"
)

/// ASSEMBLY CONFIG

assembly / mainClass := Some("play.core.server.ProdServerStart")
assembly / test := {}
assembly / assemblyJarName := "nio-provider.jar"
assembly / fullClasspath += Attributed.blank(PlayKeys.playPackageAssets.value)
assembly / assemblyMergeStrategy := {
  case PathList("javax", xs @ _*)                                             => MergeStrategy.first
  case PathList("META-INF", "native", xs @ _*)                                => MergeStrategy.first
  case PathList("org", "apache", "commons", "logging", xs @ _*)               => MergeStrategy.discard
  case PathList(xs @ _*) if xs.lastOption.contains("module-info.class")       => MergeStrategy.first
  case PathList(xs @ _*) if xs.lastOption.contains("mime.types")              => MergeStrategy.first
  case PathList(ps @ _*) if ps.last == "io.netty.versions.properties"         => MergeStrategy.first
  case PathList(ps @ _*) if ps.contains("reference-overrides.conf")           => MergeStrategy.concat
  case PathList(ps @ _*) if ps.contains("native-image.properties")            => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".conf"                          => MergeStrategy.concat
  case PathList(ps @ _*) if ps.contains("buildinfo")                          => MergeStrategy.discard
  case PathList(ps @ _*) if ps.last endsWith "reflection-config.json"         => MergeStrategy.first
  case o =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(o)
}

lazy val packageAll = taskKey[Unit]("PackageAll")
packageAll := {
  (Compile / dist).value
  (Compile / assembly).value
}

/// DOCKER CONFIG

dockerExposedPorts := Seq(
  9000
)
Docker / packageName := "nio-provider"

Docker / maintainer := "MAIF Team <maif@maif.fr>"

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
    case v                                => Seq(v)
  }

dockerEntrypoint ++= Seq(
  """-Dlogger.file=./conf/prod-logger.xml """,
  """-Dcluster.akka.remote.netty.tcp.hostname="$(eval "awk 'END{print $1}' /etc/hosts")" """,
  """-Dcluster.akka.remote.netty.tcp.bind-hostname="$(eval "awk 'END{print $1}' /etc/hosts")" """
)

dockerUpdateLatest := true

Universal / packageName := s"nio-provider"
