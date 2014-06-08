package controllers

import play.api.mvc._
import play.api.libs.json.JsValue
import models.WebSocketChannel
import play.api.Play.current

object Application extends Controller {

  def dopamine = WebSocket.acceptWithActor[JsValue, JsValue] {
    request => out =>
      WebSocketChannel.props(out)
  }

}