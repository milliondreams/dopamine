package models

import com.datastax.driver.core.Cluster

case class CassandraDB(host: List[String] = List("localhost"),
                       port: Int = 9042,
                       userName: Option[String] = Option.empty,
                       password: Option[String] = Option.empty) {
  def getSession = {
    val builder = Cluster.builder().addContactPoints(host: _*).withPort(port)
    if (userName.isDefined && password.isDefined) {
      builder.withCredentials(userName.get, password.get)
    }
    builder.build().connect()
  }
}


