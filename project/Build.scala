import sbt._
import Keys._
import com.tuplejump.sbt.yeoman.Yeoman
import play.Play.autoImport._
import PlayKeys._


object ApplicationBuild extends Build {

  val appName = "dopamine"
  val appVersion = "1.0"

  val appDependencies = Seq(
    "com.datastax.cassandra" % "cassandra-driver-core" % "1.0.4",
    "com.typesafe" % "config" % "1.0.2" % "test",
    "com.typesafe.akka" % "akka-testkit_2.10" % "2.2.3" % "test"
  )

  val appSettings = Seq(version := appVersion,
    libraryDependencies ++= appDependencies,
    scalaVersion := "2.10.4") ++ Yeoman.yeomanSettings

  val main = Project(appName, file(".")).enablePlugins(play.PlayScala).settings(
    appSettings: _*
  )

}
