import sbt.Keys._
import sbt._
import sbt.Project.projectToRef

import Dependencies._
import Settings._

def compile(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
def test(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")

lazy val clients = Seq(demoWebClient)

// a Play framework based web server
lazy val demoWebServer = (project in file("demo-web-server"))
  .settings(defaultSettings: _*)
  .settings(
    scalaJSProjects := clients,
    pipelineStages := Seq(scalaJSProd, gzip),
    includeFilter in(Assets, LessKeys.less) := "*.less",
    libraryDependencies ++=
      compile(filters, playScalajsScripts, upickle, jqueryUi, webjarsPlay, bootstrap, bootstrapTable, pkg, ccs, hcd2) ++
        test(specs2)
  )
  .enablePlugins(PlayScala, SbtWeb)
  .aggregate(clients.map(projectToRef): _*)
  .dependsOn(demoWebSharedJvm)

// a Scala.js based web client that talks to the Play server
lazy val demoWebClient = (project in file("demo-web-client")).settings(
  scalaVersion := Dependencies.ScalaVersion,
  persistLauncher := true,
  persistLauncher in Test := false,
  unmanagedSourceDirectories in Compile := Seq((scalaSource in Compile).value),
  libraryDependencies ++= clientDeps.value,
  skip in packageJSDependencies := false,
  jsDependencies ++= clientJsDeps.value
).settings(formatSettings: _*)
  .enablePlugins(ScalaJSPlugin, ScalaJSPlay)
  .dependsOn(demoWebSharedJs)


// contains simple case classes used for data transfer that are shared between the client and server
lazy val demoWebShared = (crossProject.crossType(CrossType.Pure) in file("demo-web-shared"))
  .settings(scalaVersion := Dependencies.ScalaVersion)
  .settings(formatSettings: _*)
  .jsConfigure(_ enablePlugins ScalaJSPlay)

lazy val demoWebSharedJvm = demoWebShared.jvm
lazy val demoWebSharedJs = demoWebShared.js

// loads the Play project at sbt startup
onLoad in Global := (Command.process("project demoWebServer", _: State)) compose (onLoad in Global).value
