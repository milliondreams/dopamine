package models

sealed trait WebSocketMessages

case class ConnectToCassandra(db: CassandraDB) extends WebSocketMessages

case class DBQuery(query: String, queryId: Long) extends WebSocketMessages

case class Disconnect() extends WebSocketMessages
