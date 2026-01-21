val scala3Version = "3.7.4"

lazy val root = project
  .in(file("."))
  .settings(
    name := "avocado-tracker",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.0.0" % Test,
      "com.lihaoyi" %% "requests" % "0.9.2",
      "com.lihaoyi" %% "upickle" % "4.4.2",
      "com.lihaoyi" %% "os-lib" % "0.11.6"
    )
  )
