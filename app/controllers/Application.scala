package controllers

import play.api.mvc._
import play.api.libs.json.{Json, JsString, JsObject, JsValue}
import play.api.libs.iteratee._
import akka.actor.{ActorRef, ActorLogging, Props, Actor}
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.concurrent._
import ExecutionContext.Implicits.global
import org.joda.time.DateTime
import scala.Some

object Application extends Controller {

  def index = Action {
    request =>
    //Ok(views.html.index("Your new application is ready."))
      Redirect("/ui")
  }

  def dbSocket(host: List[String], port: Option[Int] = Some(9042),
               username: Option[String] = None, password: Option[String] = None) =
    WebSocket.async[JsValue] {
      request =>
        CommandActor.init //(host, port, username, password)
    }

}

class CommandActor extends Actor with ActorLogging {

  import com.tuplejump.dopamine.cas.{CasActor, Connect, Query, QueryResult, Disconnect, Connected, ErrorWithFailure}
  import scala.concurrent.duration._
  import akka.util.Timeout
  import akka.pattern.ask

  implicit val timeout = Timeout(5 seconds)

  val (dbEnumerator, dbChannel) = Concurrent.broadcast[JsValue]

  val casActor = Akka.system.actorOf(Props[CasActor])

  def receive = {
    idle.orElse(inform).orElse(handleError)
  }

  def idle: PartialFunction[Any, Unit] = {
    case Join => {
      log.info("Entering join...")
      sender ! CasSocket(dbEnumerator)
      context.become(disconnected orElse inform orElse handleError)
    }

    case x => {
      log.error("Invalid message" + x.toString)
    }
  }

  private def disconnected: PartialFunction[Any, Unit] = {
    case c: Connect => {
      val originalSender = sender
      (casActor ? c) map {
        case c: Connected => {
          originalSender ! c
          context.become(connected orElse inform orElse handleError)
        }
      }
    }
  }

  private def connected: PartialFunction[Any, Unit] = {
    case q: Query => {
      casActor ! q
    }

    case d: Disconnect => {
      casActor ! d
      context.unbecome
    }

    case r: QueryResult => {
      dbChannel.push(Json.obj("status" -> "response", "payload" -> r.response))
    }
  }

  private def handleError: PartialFunction[Any, Unit] = {
    case e: ErrorWithFailure => {
      log.error(e.failure.failed.get, "Error connecting to cassandra")
      dbChannel.push(Json.obj("status" -> "error", "payload" -> e.msg))
    }
  }

  private def inform: PartialFunction[Any, Unit] = {
    case n: Notice => {
      dbChannel.push(Json.obj("status" -> "message", "payload" -> n.msg))
    }
  }

}

object CommandActor {

  import scala.concurrent.duration._
  import akka.util.Timeout
  import akka.pattern.ask
  import play.api.Logger
  import com.tuplejump.dopamine.cas._
  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  implicit val timeout = Timeout(5 seconds)
  implicit val connectReads = Json.reads[Connect]

  def init: scala.concurrent.Future[(Iteratee[JsValue, _], Enumerator[JsValue])] = {

    val actor = Akka.system.actorOf(Props[CommandActor])

    (actor ? Join) map {
      case CasSocket(enumerator) => {
        Logger.info("Connected to actor " + actor.path)
        val iteratee = Iteratee.foreach[JsValue] {
          msg =>
            Logger.info("Got Message -- " + msg.toString())
            (msg \ "command").asOpt[String].getOrElse("_error") match {
              case "connect" => actor ! (msg \ "payload").validate[Connect].fold(
                valid = (c => actor ! c),
                invalid = (e => actor ! Notice("Invalid Connection JSON\n" + e.toString()))
              )
              case "query" => actor ! Query(DateTime.now().getMillis.toString, (msg \ "payload" \ "query").as[String])
              case "_error" => {
                actor ! Notice("Invalid Message. No command found!")
              }
            }
        }.map {
          _ =>
            actor ! Disconnect(DateTime.now().getMillis.toString)
        }

        (iteratee, enumerator)
      }

      case ErrorWithFailure(error, e) => {
        Logger.info("Connection failed")

        // A finished Iteratee sending EOF
        val iteratee = Done[JsValue, Unit]((), Input.EOF)

        // Send an error and close the socket
        val enumerator = Enumerator[JsValue](JsObject(Seq("error" -> JsString(error))))
          .andThen(Enumerator.enumInput(Input.EOF))

        (iteratee, enumerator)
      }

      case x => {
        Logger.info("Error coneccting!!!" + x.toString)

        // A finished Iteratee sending EOF
        val iteratee = Done[JsValue, Unit]((), Input.EOF)

        // Send an error and close the socket
        val enumerator = Enumerator[JsValue](JsObject(Seq("error" -> JsString(x.toString))))
          .andThen(Enumerator.enumInput(Input.EOF))

        (iteratee, enumerator)

      }

    }
  }
}

case class CasSocket(dbe: Enumerator[JsValue])

case class Join()

case class Notice(msg: String)