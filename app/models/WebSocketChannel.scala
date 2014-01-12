package models

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import scala.concurrent.{Future, ExecutionContext}
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.libs.json.{Json, JsValue}
import play.api.libs.concurrent.Akka
import com.datastax.driver.core.ResultSet
import play.api.Play.current
import ExecutionContext.Implicits.global

class WebSocketChannel(wsChannel: Concurrent.Channel[JsValue])
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
      wsChannel.push(jsonResult)
    case message: String =>
      val result = Json.obj("status" -> message)
      wsChannel.push(result)
    case error:ErrorWithDetails =>
      wsChannel.push(Json.toJson(error))
  }
}

object WebSocketChannel {
  def props(channel: Concurrent.Channel[JsValue]): Props =
    Props(classOf[WebSocketChannel], channel)

  def init: Future[(Iteratee[JsValue, _], Enumerator[JsValue])] = {

    var actor: ActorRef = null
    val out = Concurrent.unicast[JsValue] {
      channel =>
        actor = Akka.system.actorOf(WebSocketChannel.props(channel))
    }

    Future {
      val in = Iteratee.foreach[JsValue] {
        jsReq => actor ! jsReq
      }
      (in, out)
    }
  }
}
