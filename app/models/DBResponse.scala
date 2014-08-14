package models

import com.datastax.driver.core.ResultSet
import play.api.libs.json.{Json, JsValue}

sealed trait DBResponse {
  def toJson: JsValue
}

case class QueryResult(resultSet: ResultSet, queryId: Long) extends DBResponse {
  override def toJson = Json.obj("status" -> "QueryResult",
    "payload" -> Utils.resultToJson(resultSet), "queryId" -> queryId)
}

case class InvalidQuery(queryId: Long, errorMsg: String) extends DBResponse {
  override def toJson = Json.obj("status" -> "InvalidQuery",
    "queryId" -> queryId, "errorMsg" -> errorMsg)
}

case class ConnectionFailure(errorMsg: String) extends DBResponse {
  override def toJson = Json.obj("status" -> "Connection Failure", "errorMsg" -> errorMsg)
}

class DBStatus(status: String) extends DBResponse {
  override def toJson: JsValue = Json.obj("status" -> status)
}

case class Connected(status: String = "Connected") extends DBStatus(status)
case class Disconnected(status: String = "Disonnected") extends DBStatus(status)
