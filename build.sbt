enablePlugins(JavaAppPackaging)

ThisBuild / scalaVersion := "3.5.0"

libraryDependencies ++= Seq(
  "no.arktekk.bloque" %% "jetty" % "0.1.0-SNAPSHOT",
  "no.arktekk.bloque" %% "pg" % "0.1.0-SNAPSHOT",
  "com.zaxxer" % "HikariCP" % "5.0.1",
  "org.jsoup" % "jsoup" % "1.14.3",
  "org.flywaydb" % "flyway-core" % "8.2.2",
  "org.slf4j" % "slf4j-simple" % "2.0.5",
  "org.scalameta" %% "munit" % "0.7.29" % Test
)
