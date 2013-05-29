package com.tuplejump.dopamine.cas

import akka.actor.{Props, ActorLogging, Actor}
import play.api.libs.iteratee._
import play.api.libs.concurrent.Akka
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import play.api.Play.current
import scala.concurrent._
import ExecutionContext.Implicits.global


object CommandUtil {
  implicit val errorWithFailureWrites = (
    (__ \ "reqId").write[String] and
      (__ \ "message").write[String] and
      (__ \ "error").write[String].contramap((f: scala.util.Failure[Throwable]) => f.toString)
    )(unlift(ErrorWithFailure.unapply))


  def errorMsg(e: ErrorWithFailure): JsObject = {
    Json.obj("status" -> "error", "payload" -> e)
  }

  implicit val queryResultWrites = Json.writes[QueryResult]
}

class CommandActor extends Actor with ActorLogging {

  import CommandUtil._

  implicit val timeout = Timeout(5 seconds)

  val (dbEnumerator, dbChannel) = Concurrent.broadcast[JsValue]

  val casActor = Akka.system.actorOf(Props[CasActor])

  def receive = {
    idle.orElse(inform).orElse(handleError)
  }


  def idle: PartialFunction[Any, Unit] = {
    case j: Join => {
      log.info("Entering join for " + j.reqId)
      context.become(disconnected.orElse(inform).orElse(handleError))
      sender ! CasSocket(dbEnumerator)
    }
  }

  private def disconnected: PartialFunction[Any, Unit] = {
    case c: Connect => {
      log.info("Connecting now . . .")
      (casActor ? c) map {
        case c: Connected => {
          Logger.info("CONNECTED . . .")
          dbChannel.push(Json.obj("status" -> "success", "payload" -> Json.obj("reqId" -> c.reqId)))
          context.become(connected.orElse(inform).orElse(handleError))
        }

        case e: ErrorWithFailure => {
          log.info(e.toString)
          dbChannel.push(errorMsg(e))
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
      dbChannel.push(Json.obj("status" -> "response", "payload" -> r))
    }
  }

  private def handleError: PartialFunction[Any, Unit] = {
    case e: ErrorWithFailure => {
      log.error(e.failure.failed.get, "Error connecting to cassandra")
      dbChannel.push(errorMsg(e))
    }

    case b: scala.runtime.BoxedUnit => {
      //These are control messages sent in become/unbecome... so ignore them
      log.info("Control message")
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

  import CommandUtil._

  implicit val timeout = Timeout(5 seconds)
  implicit val connectReads = Json.reads[Connect]
  implicit val queryReads = Json.reads[Query]

  def init(reqId: String): scala.concurrent.Future[(Iteratee[JsValue, _], Enumerator[JsValue])] = {

    val actor = Akka.system.actorOf(Props(new CommandActor()))

    (actor ? Join(reqId)) map {

      case CasSocket(enumerator) => {

        Logger.info("Connected to actor " + actor.path)
        val iteratee = Iteratee.foreach[JsValue] {

          msg =>
            Logger.info("Got Message -- " + msg.toString())

            (msg \ "command").asOpt[String].getOrElse("_error") match {

              case "connect" => actor ! (msg \ "payload").validate[Connect].fold(
                valid = (c => {
                  Logger.info("Connecting to %s".format(c.contactPoints))
                  Logger.info("Through actor %s".format(actor))
                  actor ! c
                }),
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

      case e: ErrorWithFailure => {
        Logger.info("Connection failed")

        // A finished Iteratee sending EOF
        val iteratee = Done[JsValue, Unit]((), Input.EOF)

        // Send an error and close the socket
        val enumerator = Enumerator[JsValue](errorMsg(e))
          .andThen(Enumerator.enumInput(Input.EOF))

        (iteratee, enumerator)
      }

      case x => {
        Logger.info("Error connecting!!!" + x.toString)

        // A finished Iteratee sending EOF
        val iteratee = Done[JsValue, Unit]((), Input.EOF)

        // Send an error and close the socket
        val enumerator = Enumerator[JsValue](JsObject(Seq("error" -> JsString(x.toString))))
          .andThen(Enumerator.enumInput(Input.EOF))

        (iteratee, enumerator)

      }

    }
  }

  import akka.actor.{Actor, DeadLetter, Props}

  class Listener extends Actor {
    def receive = {
      case d: DeadLetter ⇒ Logger.warn("DEAD_LETTER" + d.toString)
    }
  }

  val listener = Akka.system.actorOf(Props[Listener])
  Akka.system.eventStream.subscribe(listener, classOf[DeadLetter])
}

case class CasSocket(dbe: Enumerator[JsValue])

case class Join(reqId: String)

case class Notice(status: String, msg: String)