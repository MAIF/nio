// The Play plugin
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.0")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2") // Apache 2.0

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.15.0") // Apache 2.0

addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.6.1") // Apache 2.0

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.10.0") // Apache 2.0

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0") // Apache 2.0

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.13") // Apache 2.0

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always