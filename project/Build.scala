import play.Project._
import sbt._
import Keys._
import com.tuplejump.sbt.yeoman._
import sbt.ExclusionRule

object ApplicationBuild extends Build {

  val appName = "dopamine"
  val appVersion = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    //jdbc,
    //anorm
    "com.datastax.cassandra" % "cassandra-driver-core" % "1.0.0" excludeAll (ExclusionRule(organization = "org.slf4j"))
  )

  val appSettings = Yeoman.yeomanSettings ++
    Seq(resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/")


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
    appSettings: _*
  )

}
