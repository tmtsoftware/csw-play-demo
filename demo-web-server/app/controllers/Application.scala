package controllers

import akka.actor.ActorSystem
import javax.inject._

import play.api.mvc._
import akka.stream.Materializer
import play.api.Environment
import play.api.libs.streams.ActorFlow

// Main controller
@Singleton
class Application @Inject() (env: Environment, system: ActorSystem, materializer: Materializer, webJarAssets: WebJarAssets, components: ControllerComponents)
    extends AbstractController(components) {

  // websocket to client
  def ws = WebSocket.accept[String, String] { request =>
    implicit val sys = this.system
    implicit val mat = this.materializer
    ActorFlow.actorRef(out => ApplicationActor.props(out))
  }

  // Main entry point
  def index = {
    implicit val environment = env
    Action { implicit request: RequestHeader =>

      Ok(views.html.index(webJarAssets))
    }
  }
}

