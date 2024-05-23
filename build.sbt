ThisBuild / scalaVersion := "2.13.12"
ThisBuild / organization := "com.cesar.elasticsearch"


val AkkaVersion = "2.8.5"
val AkkaHttpVersion = "10.5.3"

lazy val root = (project in file("."))
  .settings(
    name := "elasticsearch-test",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
      "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
      "co.elastic.clients" % "elasticsearch-java" % "8.12.2",
      "com.nimbusds" % "nimbus-jose-jwt" % "9.37.3",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.16.1",
"com.fasterxml.uuid" % "java-uuid-generator" % "4.3.0",
    )
  )
