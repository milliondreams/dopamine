package controllers

import play.api.mvc._
import play.api.libs.json.JsValue
import models.WebSocketChannel

object Application extends Controller {

  def dopamine = WebSocket.async[JsValue] {
    request =>
      WebSocketChannel.init
  }

}