name := """nio"""
organization := "fr.maif"
scalaVersion := "2.12.4"

version := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .aggregate(
    `nio-server`
  )

lazy val `nio-server` = project