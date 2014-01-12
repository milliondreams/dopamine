import sbt._
import com.tuplejump.sbt.yeoman.Yeoman


object ApplicationBuild extends Build {

  val appName = "dopamine"
  val appVersion = "1.0"

  val appDependencies = Seq(
    "com.datastax.cassandra" % "cassandra-driver-core" % "1.0.4",
    "com.typesafe" % "config" % "1.0.2" % "test",
    "com.typesafe.akka" % "akka-testkit_2.10" % "2.2.3" % "test"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    Yeoman.yeomanSettings: _*
  )

}
