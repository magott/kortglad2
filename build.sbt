ThisBuild / scalaVersion := "3.1.0"

lazy val kortglad = project
  .in(file("."))
  .enablePlugins(JavaAppPackaging)
  .dependsOn(bloque)
  .settings(
    libraryDependencies ++= Seq(
      "org.jsoup" % "jsoup" % "1.14.3"
    )
  )

lazy val bloque = ProjectRef(
  uri("https://github.com/teigen/bloque.git#66271cd"),
  "party"
)
