name := """nio-manual"""
organization := "fr.maif"
version := "2.0.4"
scalaVersion := "2.12.4"

lazy val docs = (project in file("."))
  .enablePlugins(ParadoxPlugin)
  .settings(
    name := "Nio",
    paradoxTheme := Some(builtinParadoxTheme("generic"))
)
