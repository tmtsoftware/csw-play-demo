package controllers

import akka.actor.ActorSystem
import javax.inject._

import com.typesafe.scalalogging.slf4j.LazyLogging
import play.api.mvc._
import akka.stream.Materializer
import play.api.libs.streams.ActorFlow

// Main controller
@Singleton
class Application @Inject() (implicit system: ActorSystem, materializer: Materializer) extends Controller with LazyLogging {

  // websocket to client
  def ws = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef(out => ApplicationActor.props(out))
  }

  // Main entry point
  def index = {
    Action { implicit request =>
      Ok(views.html.index())
    }
  }
}

