  ThisBuild / version := "0.1.0-SNAPSHOT"

  ThisBuild / scalaVersion := "2.13.11"

  lazy val root = (project in file("."))
    .settings(
      name := "dataprocessing",
      libraryDependencies ++= Seq(
        "org.apache.spark" %% "spark-core" % "3.4.1",
        "org.apache.spark" %% "spark-sql" % "3.4.1",
        "org.apache.spark" %% "spark-mllib" % "3.4.1",
        "org.glassfish" % "javax.json" % "1.1.4",
        "com.github.scopt" %% "scopt" % "4.1.0",
        "org.json" % "json" % "20230227",
        "ch.qos.logback" % "logback-classic" % "1.4.7",
        "org.apache.logging.log4j" % "log4j-core" % "2.20.0"

      ),
      assembly / assemblyMergeStrategy := {
        case PathList("META-INF","services",xs @ _*) => MergeStrategy.filterDistinctLines
        case PathList("META-INF", xs@_*) => MergeStrategy.discard
        case x => MergeStrategy.first
      },
      assembly / mainClass:= Some("Main")
    )
