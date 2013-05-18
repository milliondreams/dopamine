package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {

  def index = Action {
    request =>
    //Ok(views.html.index("Your new application is ready."))
    Redirect("/ui")
  }

  def dbConnect() = {

  }

  /* def runQuery(query:String) = Action {

  }  */

}