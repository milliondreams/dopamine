package com.tuplejump.dopamine.cas

import com.datastax.driver.core.Cluster

/**
 * Created with IntelliJ IDEA.
 * User: rohit
 * Date: 5/15/13
 * Time: 6:56 PM
 * To change this template use File | Settings | File Templates.
 */
class CassandraDB(node: String) {

  private lazy val cluster = {
    Cluster.builder()
      .addContactPoint(node).build()
  }

  private lazy val session =
    cluster.connect()


  def connect() = {
    session
  }

  def query(query: String) = {
    session.execute(query)
  }

  def queryAsync(query: String) = {
    session.executeAsync(query)
  }


  def close = {
    cluster.shutdown()
  }
}
