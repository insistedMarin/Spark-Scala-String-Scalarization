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
        "com.github.scopt" %% "scopt" % "4.1.0"

      )
    )
