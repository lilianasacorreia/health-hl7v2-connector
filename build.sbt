import sbtassembly.AssemblyPlugin.autoImport._
import sbtassembly.{MergeStrategy, PathList}

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.7"


lazy val pekkoVersion = "1.2.1"

val hapi = "ca.uhn.hapi"

val hapiFhir = hapi + ".fhir"
val hapiFhirVersion = "7.0.3"
val hapiFhirBase = hapiFhir % "hapi-fhir-base" % hapiFhirVersion
val hapiFhirStructuresR5 = hapiFhir % "hapi-fhir-structures-r5" % hapiFhirVersion
val hapiFhirStructuresR4B = hapiFhir % "hapi-fhir-structures-r4b" % hapiFhirVersion
val hapiFhirStructuresR4 = hapiFhir % "hapi-fhir-structures-r4" % hapiFhirVersion

val hapiVersion = "2.5.1"
val hapiBase = hapi % "hapi-base" % hapiVersion
val hapiStructuresV25 = hapi % "hapi-structures-v25" % hapiVersion
val scalaTest = "org.scalatest" %% "scalatest" % "3.2.15" % Test

val vavr = "io.vavr" % "vavr" % "0.10.0"
val slf4j = "org.slf4j" % "jul-to-slf4j" % "2.0.13"
val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.5.6"
val sql = "com.oracle.database.jdbc" % "ojdbc8" % "19.8.0.0"
val hikaricp = "com.zaxxer" % "HikariCP" % "5.0.1"

lazy val root = (project in file("."))
  .settings(
    name := "HealthDataHl7v2Conector",
    Compile / mainClass := Some("Server"),
    assembly / mainClass := Some("Server"),
    assembly / assemblyMergeStrategy := {
      case PathList("module-info.class") =>
        MergeStrategy.discard

      case PathList("META-INF", "versions", "9", "module-info.class") =>
        MergeStrategy.discard

      case PathList("version.conf") =>
        MergeStrategy.first

      case PathList("META-INF", xs @ _*) =>
        // normalmente não precisas destes meta-dados no fat jar
        MergeStrategy.discard

      case x =>
        val old = (assembly / assemblyMergeStrategy).value
        old(x)
    },

    libraryDependencies ++= Seq(
      // Pekko
      "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
      "org.apache.pekko" %% "pekko-stream" % pekkoVersion,
      "org.apache.pekko" %% "pekko-http" % "1.1.0",
      "org.apache.pekko" %% "pekko-connectors-kafka" % "1.0.0",
      hapiFhirBase,
      hapiFhirStructuresR5,
      hapiFhirStructuresR4B,
      hapiFhirStructuresR4,
      hapiBase,
      hapiStructuresV25,
      scalaTest,
      vavr,
      slf4j,
      logbackClassic
    ),
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked"
    )
  )
