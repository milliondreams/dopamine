package controllers

import play.api.mvc._
import play.api.libs.json.JsValue
import scala.Some
import com.tuplejump.dopamine.cas.CommandActor

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

