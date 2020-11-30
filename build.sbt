import ReleaseTransformations._

name := """nio"""
organization := "fr.maif"
scalaVersion := "2.13.3"

lazy val root = (project in file("."))
  .aggregate(
    `nio-server`,
    `nio-provider`
  )

lazy val `nio-server`   = project
lazy val `nio-provider` = project

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies, // : ReleaseStep
  inquireVersions,           // : ReleaseStep
  runClean,                  // : ReleaseStep
  //runTest, // : ReleaseStep
  setReleaseVersion,         // : ReleaseStep
  commitReleaseVersion,      // : ReleaseStep, performs the initial git checks
  tagRelease,                // : ReleaseStep
  //publishArtifacts,                       // : ReleaseStep, checks whether `publishTo` is properly set up
  setNextVersion,            // : ReleaseStep
  commitNextVersion,         // : ReleaseStep
  pushChanges                // : ReleaseStep, also checks that an upstream branch is properly configured
)
