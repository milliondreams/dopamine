package models

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import play.api.libs.json.{Json, JsValue}
import play.api.libs.concurrent.Akka
import com.datastax.driver.core.ResultSet
import play.api.Play.current

class WebSocketChannel(out: ActorRef)
  extends Actor with ActorLogging {

  val backend = Akka.system.actorOf(DBActor.props)
  implicit val dbRead = Json.reads[CassandraDB]
  implicit val queryRead = Json.reads[DBQuery]
  implicit val errorWrite = Json.writes[ErrorWithDetails]

  private def convertJson(jsRequest: JsValue): Any = {
    val requestType = (jsRequest \ "messageType").as[String]
    var message: Any = null
    requestType match {
      case "connect" =>
        val casDB = Json.fromJson[CassandraDB](jsRequest).asOpt.get
        message = new ConnectToCassandra(casDB)
      case "disconnect" =>
        message = new Disconnect()
      case "query" =>
        message = Json.fromJson[DBQuery](jsRequest).asOpt.get
    }
    message
  }

  def receive: Actor.Receive = {
    case jsRequest: JsValue =>
      backend ! convertJson(jsRequest)
    case QueryResult(resultSet: ResultSet, queryId: Long) =>
      val jsonResult = Json.obj("status" -> "QueryResult",
        "payload" -> Utils.resultToJson(resultSet), "queryId" -> queryId)
      out ! jsonResult
    case message: String =>
      val result = Json.obj("status" -> message)
      out ! result
    case error: ErrorWithDetails =>
      out ! Json.toJson(error)
  }
}

object WebSocketChannel {
  def props(out: ActorRef): Props =
    Props(classOf[WebSocketChannel], out)

}
