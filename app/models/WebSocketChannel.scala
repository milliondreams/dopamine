package models

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.json.{JsValue, Json}

class WebSocketChannel(out: ActorRef)
  extends Actor with ActorLogging {

  val backend = Akka.system.actorOf(DBActor.props)
  implicit val dbRead = Json.reads[CassandraDB]
  implicit val queryRead = Json.reads[DBQuery]

  private def convertJson(jsRequest: JsValue): WebSocketMessages = {
    val requestType = (jsRequest \ "messageType").as[String]
    var message: WebSocketMessages = null
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
    case x:DBResponse =>
      out ! x.toJson
    case message: String =>
      val result = Json.obj("status" -> message)
      out ! result
  }
}

object WebSocketChannel {
  def props(out: ActorRef): Props =
    Props(classOf[WebSocketChannel], out)

}
