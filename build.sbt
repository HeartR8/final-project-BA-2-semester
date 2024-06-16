import Dependencies._

ThisBuild / scalaVersion := "2.13.12"
ThisBuild / version := "0.0.1-SNAPSHOT"

def dockerSettings(name: String) = Seq(
  dockerBaseImage := "openjdk:11",
  Docker / packageName := name,
  Docker / dockerExposedPorts := Seq(),
  dockerBuildOptions += "--quiet"
)

lazy val root = (project in file("."))
  .settings(
    name := "final-project"
  )
  .aggregate(hotels, tickets, tours)

lazy val hotels = (project in file("hotels"))
  .settings(
    name := "hotels",
    libraryDependencies ++= FinalProject.dependencies,
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    scalacOptions ++= Seq(
      "-language:higherKinds",
      "-Ymacro-annotations"
    ),
    Compile / run / fork := true
  )
  .enablePlugins(JavaAppPackaging)
  .settings(dockerSettings("hotels"))

lazy val tickets = (project in file("tickets"))
  .settings(
    name := "tickets",
    libraryDependencies ++= FinalProject.dependencies,
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    scalacOptions ++= Seq(
      "-language:higherKinds",
      "-Ymacro-annotations"
    ),
    Compile / run / fork := true
  )
  .enablePlugins(JavaAppPackaging)
  .settings(dockerSettings("tickets"))

lazy val tours = (project in file("tours"))
  .settings(
    name := "tours",
    libraryDependencies ++= FinalProject.dependencies,
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    scalacOptions ++= Seq(
      "-language:higherKinds",
      "-Ymacro-annotations"
    ),
    Compile / run / fork := true
  )
  .enablePlugins(JavaAppPackaging)
  .settings(dockerSettings("tours"))