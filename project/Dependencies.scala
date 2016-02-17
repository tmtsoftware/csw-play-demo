import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._

object Dependencies {
  val Version = "0.9"
  val ScalaVersion = "2.11.7"

  // web server dependencies
  val playScalajsScripts = "com.vmunier" %% "play-scalajs-scripts" % "0.4.0"
  val upickle = "com.lihaoyi" %% "upickle" % "0.3.8"
  val jqueryUi = "org.webjars" % "jquery-ui" % "1.11.4"
  val webjarsPlay = "org.webjars" %% "webjars-play" % "2.4.0-1"
  val bootstrap = "org.webjars" % "bootstrap" % "3.3.4"
  val bootstrapTable = "org.webjars.bower" % "bootstrap-table" % "1.7.0"
  val pkg = "org.tmt" %% "pkg" % "0.2-SNAPSHOT"

  // ScalaJS web client scala dependencies
  val clientDeps = Def.setting(Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.0",
    "com.lihaoyi" %%% "scalatags" % "0.5.4",
    "com.lihaoyi" %%% "upickle" % "0.3.8",
    "org.querki" %%% "jquery-facade" % "0.11", // includes jquery webjar!
    "com.github.japgolly.scalacss" %%% "core" % "0.3.1",
    "com.github.japgolly.scalacss" %%% "ext-scalatags" % "0.3.1"
  ))

  // ScalaJS client JavaScript dependencies
  val clientJsDeps = Def.setting(Seq(
    "org.webjars" % "bootstrap" % "3.3.4" / "bootstrap.min.js" dependsOn "jquery.js",
    "org.webjars.bower" % "bootstrap-table" % "1.7.0" / "bootstrap-table.min.js"
  ))
}

