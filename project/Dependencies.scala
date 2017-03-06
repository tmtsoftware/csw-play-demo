import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._

object Dependencies {
  val Version = "0.4-SNAPSHOT"
  val ScalaVersion = "2.12.1"

  // web server dependencies
  val scalajsScripts = "com.vmunier" %% "scalajs-scripts" % "1.1.0"
  val upickle = "com.lihaoyi" %% "upickle" % "0.4.4"
  val jqueryUi = "org.webjars" % "jquery-ui" % "1.12.0"
  val webjarsPlay = "org.webjars" %% "webjars-play" % "2.6.0-M1"
  val bootstrap = "org.webjars" % "bootstrap" % "3.3.7"
  val bootstrapTable = "org.webjars.bower" % "bootstrap-table" % "1.11.0"
  val ccs = "org.tmt" %% "ccs" % Version
  val pkg = "org.tmt" %% "pkg" % Version
  val hcd2 = "org.tmt" %% "hcd2" % Version

  // ScalaJS web client scala dependencies
  val clientDeps = Def.setting(Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.1",
    "com.lihaoyi" %%% "scalatags" % "0.6.3",
    "com.lihaoyi" %%% "upickle" % "0.4.4",
    "org.querki" %%% "jquery-facade" % "1.0",
    "com.github.japgolly.scalacss" %%% "core" % "0.5.1",
    "com.github.japgolly.scalacss" %%% "ext-scalatags" % "0.5.1"
  ))

  // ScalaJS client JavaScript dependencies
  val clientJsDeps = Def.setting(Seq(
    "org.webjars" % "jquery" % "3.1.0" / "jquery.js" minified "jquery.min.js",
    "org.webjars" % "bootstrap" % "3.3.7" / "bootstrap.min.js" dependsOn "jquery.js",
    "org.webjars.bower" % "bootstrap-table" % "1.11.0" / "bootstrap-table.min.js"
  ))
}
