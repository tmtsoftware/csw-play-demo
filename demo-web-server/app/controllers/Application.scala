package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import csw.services.cmd.spray.CommandServiceHttpClient
import csw.util.cfg.Configurations.SetupConfig
import play.api._
import play.api.Play.current
import play.api.libs.iteratee.{ Concurrent, Enumerator, Iteratee }
import demo.web.shared.Csrf
import play.api.mvc._
import play.filters.csrf.CSRFAddToken
import play.api.libs.json._

// Main controller: redirects to the Assembly1 page
object Application extends Controller {

  // Command service http client (XXX could also be replaced by actor client)
  case class CommandService(host: String, port: Int) extends CommandServiceHttpClient {
    // XXX can these be handled differently?
    override val system = ActorSystem(s"CommandService-$host-$port")
    override val dispatcher = system.dispatcher
  }

  // XXX FIXME: Get from location service
  val host = "localhost"
  val port = 8089
  val commandService = CommandService(host, port)

  // Websocket
  val (wsEnumerator, wsChannel) = Concurrent.broadcast[String]

  def ws = WebSocket.using[String] { request ⇒
    (Iteratee.ignore, wsEnumerator)
  }

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
    implicit val system = commandService.system
    import system.dispatcher
    implicit val materializer = ActorMaterializer()

    val obsId = "obs0001" // XXX TODO FIXME

    val filterConfig = filterOpt.map { filter ⇒
      SetupConfig(
        obsId = obsId,
        "tmt.mobie.blue.filter",
        "value" -> filter)
    }

    val disperserConfig = disperserOpt.map { disperser ⇒
      SetupConfig(
        obsId = obsId,
        "tmt.mobie.blue.disperser",
        "value" -> disperser)
    }

    val configs = List(filterConfig, disperserConfig).flatten
    println(s"XXX configs = $configs")

    for {
      source ← commandService.queueSubmit(configs)
    } yield {
      source.runForeach { status ⇒
        println(s"XXX Command status: $status")
        wsChannel.push(status.name)
      }
    }

    // XXX Error checking? (Execution errors will be sent by websocket later)
    Ok
  }
}
