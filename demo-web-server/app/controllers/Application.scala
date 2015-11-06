package controllers

import akka.actor.ActorSystem
import javax.inject._
import com.typesafe.scalalogging.slf4j.LazyLogging
import play.api.libs.iteratee.{ Concurrent, Iteratee }
import demo.web.shared.Csrf
import play.api.mvc._
import play.filters.csrf.CSRFAddToken
import ApplicationActor._

// Main controller
@Singleton
class Application @Inject() (system: ActorSystem) extends Controller with LazyLogging {

  // Websocket
  val (wsEnumerator, wsChannel) = Concurrent.broadcast[String]

  def ws = WebSocket.using[String] { request ⇒
    (Iteratee.ignore, wsEnumerator)
  }

  // start the aplication actor
  val appActor = system.actorOf(ApplicationActor.props)

  // Main entry point
  def index = CSRFAddToken {
    Action { implicit request ⇒
      import play.filters.csrf.CSRF
      val token = CSRF.getToken(request).map(t ⇒ Csrf(t.value)).getOrElse(Csrf(""))
      Ok(views.html.index(token))
    }
  }

  // Submit a command
  def submit(filterOpt: Option[String], disperserOpt: Option[String]) = Action {
    appActor ! Submit(wsChannel, filterOpt, disperserOpt)
    // Status messages will be sent by websocket later
    Ok("")
  }

  // Gets the current values
  def configGet() = Action {
    appActor ! ConfigGet(wsChannel)
    // Reply will be sent by websocket later
    Ok("")
  }
}

