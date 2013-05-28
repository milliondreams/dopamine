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
      log.debug("Entering join...")
      sender ! CasSocket(dbEnumerator)
      context.become(disconnected orElse inform orElse handleError)
    }

    case invalidMsg => {
      log.error("Invalid message -- " + invalidMsg.toString)
    }
  }

  private def disconnected: PartialFunction[Any, Unit] = {
    case c: Connect => {
      (casActor ? c) map {
        case c: Connected => {
          dbChannel.push(Json.obj("status" -> "success"))
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

    case b: scala.runtime.BoxedUnit => {
      //These are control messages sent in become/unbecome... so ignore them
      log.debug("Control message")
    }

    case unknown => {
      log.error("Invalid message: " + unknown.toString)
      dbChannel.push(Json.obj("status" -> "error", "payload" -> ("Unknown Message: " + unknown.toString)))
    }
  }

  private def inform: PartialFunction[Any, Unit] = {
    case n: Notice => {
      dbChannel.push(Json.obj("status" -> n.status, "payload" -> n.msg))
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
  implicit val queryReads = Json.reads[Query]

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
                invalid = (e => actor ! Notice("error", "Invalid Connection JSON \n" + e.toString()))
              )

              case "query" => (msg \ "payload").validate[Query].fold(
                valid = (q => actor ! q),
                invalid = (e => actor ! Notice("error", "Invalid Query JSON \n" + e.toString()))
              )

              case "_error" => {
                actor ! Notice("error", "Invalid Message. No command found!")
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

case class Notice(status: String, msg: String)