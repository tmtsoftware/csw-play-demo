import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import sbt.Keys._
import sbt._
import sbt.Project.projectToRef

val Version = "0.1-SNAPSHOT"
val ScalaVersion = "2.11.7"

// Basic settings
val buildSettings = Seq(
  organization := "org.tmt",
  organizationName := "TMT",
  organizationHomepage := Some(url("http://www.tmt.org")),
  version := Version,
  scalaVersion := ScalaVersion,
  crossPaths := true,
  parallelExecution in Test := false,
  fork := true,
  resolvers += Resolver.typesafeRepo("releases"),
  resolvers += Resolver.sonatypeRepo("releases"),
  resolvers += sbtResolver.value
)

// Automatic code formatting
def formattingPreferences: FormattingPreferences =
  FormattingPreferences()
    .setPreference(RewriteArrowSymbols, true)
    .setPreference(AlignParameters, true)
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(DoubleIndentClassDeclaration, true)

lazy val formatSettings = SbtScalariform.scalariformSettings ++ Seq(
  ScalariformKeys.preferences in Compile := formattingPreferences,
  ScalariformKeys.preferences in Test := formattingPreferences
)

// Using java8
lazy val defaultSettings = buildSettings ++ formatSettings ++ Seq(
  scalacOptions ++= Seq("-target:jvm-1.8", "-encoding", "UTF-8", "-feature", "-deprecation", "-unchecked"),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked", "-Xlint:deprecation")
)

lazy val clients = Seq(demoWebClient)

// Root of the multi-project build
//lazy val root = (project in file("."))
//  .aggregate(demoWebServer)

// a Play framework based web server
lazy val demoWebServer = (project in file("demo-web-server"))
  .settings(defaultSettings: _*)
  .settings(
    scalaJSProjects := clients,
    pipelineStages := Seq(scalaJSProd, gzip),
    includeFilter in(Assets, LessKeys.less) := "*.less",
    libraryDependencies ++= Seq(
      filters,
      "com.vmunier" %% "play-scalajs-scripts" % "0.3.0",
      "com.lihaoyi" %%% "upickle" % "0.3.4",
      "org.webjars" %% "webjars-play" % "2.4.0-1",
      "org.webjars" % "bootstrap" % "3.3.4",
      "org.webjars.bower" % "bootstrap-table" % "1.7.0",
      "org.tmt" %% "cmd" % "0.1-SNAPSHOT",
      specs2 % Test
    )
  )
  .enablePlugins(PlayScala, SbtWeb)
  .aggregate(clients.map(projectToRef): _*)
  .dependsOn(demoWebSharedJvm)

// a Scala.js based web client that talks to the Play server
lazy val demoWebClient = (project in file("demo-web-client")).settings(
  scalaVersion := ScalaVersion,
  persistLauncher := true,
  persistLauncher in Test := false,
  sourceMapsDirectories += demoWebSharedJs.base / "..",
  unmanagedSourceDirectories in Compile := Seq((scalaSource in Compile).value),
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.8.1",
    "com.lihaoyi" %%% "scalatags" % "0.5.2",
    "com.lihaoyi" %%% "upickle" % "0.3.4",
    "org.querki" %%% "jquery-facade" % "0.7", // includes jquery webjar!
    "com.github.japgolly.scalacss" %%% "core" % "0.3.0",
    "com.github.japgolly.scalacss" %%% "ext-scalatags" % "0.3.0",
    "org.tmt" %%% "shared" % "0.1-SNAPSHOT"
  ),
  skip in packageJSDependencies := false,
  jsDependencies ++= Seq(
    "org.webjars" % "bootstrap" % "3.3.4" / "bootstrap.min.js" dependsOn "jquery.js",
    "org.webjars.bower" % "bootstrap-table" % "1.7.0" / "bootstrap-table.min.js"
  )
).settings(formatSettings: _*)
  .enablePlugins(ScalaJSPlugin, ScalaJSPlay)
  .dependsOn(demoWebSharedJs)


// contains simple case classes used for data transfer that are shared between the client and server
lazy val demoWebShared = (crossProject.crossType(CrossType.Pure) in file("demo-web-shared"))
  .settings(scalaVersion := ScalaVersion)
  .settings(formatSettings: _*)
  .settings(libraryDependencies += "org.tmt" %%% "shared" % "0.1-SNAPSHOT")
  .jsConfigure(_ enablePlugins ScalaJSPlay)
  .jsSettings(sourceMapsBase := baseDirectory.value / "..")


lazy val demoWebSharedJvm = demoWebShared.jvm
lazy val demoWebSharedJs = demoWebShared.js

// loads the Play project at sbt startup
onLoad in Global := (Command.process("project demoWebServer", _: State)) compose (onLoad in Global).value
