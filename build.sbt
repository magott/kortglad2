enablePlugins(JavaAppPackaging)

ThisBuild / scalaVersion := "3.1.0"

libraryDependencies ++= Seq(
  "no.arktekk.bloque" %% "party" % "0.1.0-SNAPSHOT",
  "org.jsoup" % "jsoup" % "1.14.3",
  "org.flywaydb" % "flyway-core" % "8.2.2",
  "org.scalameta" %% "munit" % "0.7.29" % Test
)
