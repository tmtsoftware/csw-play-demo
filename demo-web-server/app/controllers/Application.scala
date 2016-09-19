package controllers

import akka.actor.ActorSystem
import javax.inject._

import com.typesafe.scalalogging.slf4j.LazyLogging
import play.api.mvc._
import akka.stream.Materializer
import play.api.Environment
import play.api.libs.streams.ActorFlow

// Main controller
@Singleton
class Application @Inject() (env: Environment, system: ActorSystem, materializer: Materializer) extends Controller with LazyLogging {

  // websocket to client
  def ws = WebSocket.accept[String, String] { request =>
    implicit val sys = this.system
    implicit val mat = this.materializer
    ActorFlow.actorRef(out => ApplicationActor.props(out))
  }

  // Main entry point
  def index = {
    implicit val environment = env
    Action { implicit request =>
      Ok(views.html.index())
    }
  }
}

