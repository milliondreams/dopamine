package controllers

import play.api.mvc._
import play.api.libs.json.JsValue
import scala.Some
import com.tuplejump.dopamine.cas.CommandActor
import org.joda.time.DateTime

object Application extends Controller {

  def dbSocket(host: List[String], port: Option[Int] = Some(9042),
               username: Option[String] = None, password: Option[String] = None) =
    WebSocket.async[JsValue] {
      request =>
        CommandActor.init(DateTime.now().getMillis.toString) //(host, port, username, password)
    }

}

