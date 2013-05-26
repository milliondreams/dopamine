package controllers

import play.api.mvc._
import play.api.libs.json.JsValue
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import akka.actor.{Props, Actor}
import play.api.libs.concurrent.Akka
import com.tuplejump.dopamine.cas._
import play.api.Play.current

object Application extends Controller {

  def index = Action {
    request =>
    //Ok(views.html.index("Your new application is ready."))
      Redirect("/ui")
  }


 /*  def dbSocket(host: List[String], port: Int = Some(9042),
               username: Option[String] = None, password: Option[String] = None) =
    WebSocket.using[String] {

      request =>

      // Log events to the console
        val in = Iteratee.foreach[String](println).mapDone {
          _ =>
            println("Disconnected")
        }

        // Send a single 'Hello!' message
        val out = Enumerator("Hello!")

        (in, out)
    } */

  /*

  def dbConnect() = Action(parse.json) {
    request =>
      request.body.validate[ConnectMessage] map {
        case x => {
          val db = new CassandraDB(x.host)
          db.connect()
          Ok("Done")
        }
      } recoverTotal {
        e => BadRequest("Detected error:" + JsError.toFlatJson(e))
      }
  }  */
}

class CommandActor(host: List[String], port: Option[Int], username: Option[String], password: Option[String]) extends Actor {

  val (chatEnumerator, chatChannel) = Concurrent.broadcast[JsValue]

  val casActor = Akka.system.actorOf(Props[CasActor])

  def receive = {
    case q: Query => {
      casActor ! q
    }

    /* case r:Result => {

    }  */
  }
}
