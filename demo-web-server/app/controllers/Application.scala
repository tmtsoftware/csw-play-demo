package controllers

import play.api._
import play.api.Play.current
import play.api.libs.iteratee.{ Concurrent, Enumerator, Iteratee }
import demo.web.shared.Csrf
import play.api.mvc._
import play.filters.csrf.CSRFAddToken
import play.api.libs.json._

// Main controller: redirects to the Assembly1 page
object Application extends Controller {

  // Websocket used to notify client when upload is complete
  val (wsEnumerator, wsChannel) = Concurrent.broadcast[String]

  def ws = WebSocket.using[String] { request ⇒
    (Iteratee.ignore, wsEnumerator)
  }

  def index = CSRFAddToken {
    Action { implicit request ⇒
      import play.filters.csrf.CSRF
      val token = CSRF.getToken(request).map(t ⇒ Csrf(t.value)).getOrElse(Csrf(""))
      Ok(views.html.index(token))
    }
  }

}
