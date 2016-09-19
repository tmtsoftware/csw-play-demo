import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._

object Dependencies {
  val Version = "0.2-SNAPSHOT"
  val ScalaVersion = "2.11.8"

  // web server dependencies
  val playScalajsScripts = "com.vmunier" %% "play-scalajs-scripts" % "0.5.0"
  val upickle = "com.lihaoyi" %% "upickle" % "0.4.1"
  val jqueryUi = "org.webjars" % "jquery-ui" % "1.12.0"
  val webjarsPlay = "org.webjars" %% "webjars-play" % "2.4.0-1"
  val bootstrap = "org.webjars" % "bootstrap" % "3.3.7"
  val bootstrapTable = "org.webjars.bower" % "bootstrap-table" % "1.11.0"
  val pkg = "org.tmt" %% "pkg" % "0.2-SNAPSHOT"
  val hcd2 = "org.tmt" %% "hcd2" % "0.2-SNAPSHOT"

  // ScalaJS web client scala dependencies
  val clientDeps = Def.setting(Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.1",
    "com.lihaoyi" %%% "scalatags" % "0.6.0",
    "com.lihaoyi" %%% "upickle" % "0.4.1",
    "org.querki" %%% "jquery-facade" % "1.0-RC6",
    "com.github.japgolly.scalacss" %%% "core" % "0.4.1",
    "com.github.japgolly.scalacss" %%% "ext-scalatags" % "0.4.1"
  ))

  // ScalaJS client JavaScript dependencies
  val clientJsDeps = Def.setting(Seq(
    "org.webjars" % "jquery" % "3.1.0" / "jquery.js" minified "jquery.min.js",
    "org.webjars" % "bootstrap" % "3.3.7" / "bootstrap.min.js" dependsOn "jquery.js",
    "org.webjars.bower" % "bootstrap-table" % "1.11.0" / "bootstrap-table.min.js"
  ))
}
