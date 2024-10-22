import Dependencies._
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

name := """nio-server"""
organization := "fr.maif"

lazy val `nio-server` = (project in file("."))
  .enablePlugins(PlayScala, DockerPlugin)
  .disablePlugins(PlayPekkoHttpServer)
  .enablePlugins(NoPublish)
  .disablePlugins(BintrayPlugin)

scalaVersion := Dependencies._scalaVersion

resolvers ++= Seq(
  Resolver.jcenterRepo,
  "Maven central" at "https://repo1.maven.org/maven2/"
)

dependencyOverrides ++= Seq(
  "com.github.luben"          % "zstd-jni"                 % "1.5.6-4",
  "org.scala-lang.modules" %% "scala-xml" % "2.2.0"
)

ThisBuild / scalafixDependencies  ++= Seq("org.reactivemongo" %% "reactivemongo-scalafix" % "1.1.0-RC13")

libraryDependencies ++= Seq(
  ws,
  nettyServer,
  "org.apache.pekko"         %% "pekko-stream"              % pekko,
  "org.apache.pekko"         %% "pekko-actor-typed"         % pekko,
  "org.apache.pekko"         %% "pekko-slf4j"               % pekko,
  "org.apache.pekko"         %% "pekko-slf4j"               % pekko,
  "org.apache.pekko"         %% "pekko-serialization-jackson" % pekko,
  "org.apache.pekko"         %% "pekko-http"                % "1.1.0",
  "org.apache.pekko"         %% "pekko-http-xml"            % "1.1.0",
  "org.apache.pekko"         %% "pekko-connectors-kafka"    % pekkoKafka,
  "org.reactivemongo"        %% "reactivemongo"             % s"$reactiveMongoVersion-pekko.RC13",
  "org.reactivemongo"        %% "play2-reactivemongo"       % s"$reactiveMongoVersion-play30.RC13",
  "org.reactivemongo"        %% "reactivemongo-pekkostream" % s"$reactiveMongoVersion-RC13",

  "org.apache.commons"        % "commons-lang3"            % "3.11",
  "de.svenkubiak"             % "jBCrypt"                  % "0.4.1", //  ISC/BSD
  "com.auth0"                 % "java-jwt"                 % javaJwt, // MIT license
//  "com.github.pureconfig"    %% "pureconfig"               % pureConfig, // Apache 2.0
  "com.github.pureconfig"    %% "pureconfig-core"          % pureConfig, // Apache 2.0
  "com.github.pureconfig"    %% "pureconfig-generic-scala3" % pureConfig, // Apache 2.0
  "org.scalactic"            %% "scalactic"                % scalaticVersion, // Apache 2.0
  "org.webjars"               % "swagger-ui"               % "3.12.1",
  "org.typelevel"            %% "cats-core"                % catsVersion, // MIT
  "com.softwaremill.macwire" %% "macros"                   % macwireVersion % "provided",
  "com.amazonaws"             % "aws-java-sdk-s3"          % "1.11.909", // Apache 2.0
  "io.dropwizard.metrics"     % "metrics-core"             % metricsVersion, // Apache 2.0
  "io.dropwizard.metrics"     % "metrics-json"             % metricsVersion, // Apache 2.0
  "io.dropwizard.metrics"     % "metrics-jvm"              % metricsVersion, // Apache 2.0

  // S3 client for akka-stream
  "org.apache.pekko"       %% "pekko-connectors-s3" % pekkoS3Version,
  "org.scalatestplus.play" %% "scalatestplus-play"     % scalatestPlay % Test
)

scalacOptions ++= Seq(
  "-feature",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:existentials",
  "-Wunused:imports" //, "-Xsource:3"
)

/// ASSEMBLY CONFIG

Test / parallelExecution := false
assembly / mainClass := Some("play.core.server.ProdServerStart")
assembly / test := {}
assembly / assemblyJarName := "nio.jar"
assembly / fullClasspath += Attributed.blank(PlayKeys.playPackageAssets.value)
assembly / assemblyMergeStrategy := {
  case PathList("javax", xs @ _*)                                             => MergeStrategy.first
  case PathList("META-INF", "native", xs @ _*)                                => MergeStrategy.first
  case PathList("org", "apache", "commons", "logging", xs @ _*)               => MergeStrategy.discard
  case PathList(xs @ _*) if xs.lastOption.contains("module-info.class")       => MergeStrategy.first
  case PathList(xs @ _*) if xs.lastOption.contains("mime.types")              => MergeStrategy.first
  case PathList(ps @ _*) if ps.last == "io.netty.versions.properties"         => MergeStrategy.first
  case PathList(ps @ _*) if ps.contains("reference-overrides.conf")           => MergeStrategy.concat
  case PathList(ps @ _*) if ps.contains("reflect-config.json")                => MergeStrategy.first
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
Docker / packageName := "nio"

Docker / maintainer := "MAIF Team <maif@maif.fr>"

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
    case v                                => Seq(v)
  }

dockerEntrypoint ++= Seq(
  """-Dlogger.file=./conf/prod-logger.xml """,
  """-Dcluster.akka.remote.netty.tcp.hostname="$(eval "awk 'END{print $1}' /etc/hosts")" """,
  """-Dcluster.akka.remote.netty.tcp.bind-hostname="$(eval "awk 'END{print $1}' /etc/hosts")" """
)

dockerUpdateLatest := true

Universal / packageName := s"nio"
