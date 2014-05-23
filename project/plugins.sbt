resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.0-RC2")

// web plugins

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.0-RC1")

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.0-RC1")

addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.0-RC1")

addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.0-RC1")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.0.0-RC1")

addSbtPlugin("com.typesafe.sbt" % "sbt-mocha" % "1.0.0-RC1")


// XXX To use a fixed version of twirl, until play 2.3.0-RC2 is released, add an extra plugin declaration:
addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % "1.0.0-RC2")
