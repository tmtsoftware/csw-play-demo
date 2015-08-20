package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.slf4j.LazyLogging
import csw.services.cmd.spray.CommandServiceHttpClient
import csw.util.cfg.Configurations.SetupConfig
import play.api.libs.iteratee.{ Concurrent, Iteratee }
import demo.web.shared.{ WebSocketMessage, DemoData, Csrf }
import play.api.mvc._
import play.filters.csrf.CSRFAddToken

import scala.util.{ Failure, Success }

// Main controller: redirects to the Assembly1 page
object Application extends Controller with LazyLogging {

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
    import upickle.default._
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

    for {
      source ← commandService.queueSubmit(configs)
    } yield {
      source.runForeach { status ⇒
        val msg = WebSocketMessage(status = Some(status))
        val json = write(msg)
        wsChannel.push(json)
      }
    }

    // Error checking? (Execution errors will be sent by websocket later)
    Ok("")
  }

  // Gets the current values
  def configGet() = Action {
    import upickle.default._
    implicit val system = commandService.system
    import system.dispatcher
    implicit val materializer = ActorMaterializer()

    val obsId = "obs0001" // XXX TODO FIXME

    // XXX TODO: Could specify only the parts we are interested in (filter or disperser)...
    val filterConfig = SetupConfig(
      obsId = obsId,
      "tmt.mobie.blue.filter",
      "value" -> "")

    val disperserConfig = SetupConfig(
      obsId = obsId,
      "tmt.mobie.blue.disperser",
      "value" -> "")

    val configs = List(filterConfig, disperserConfig)

    val f = for {
      setupConfigList ← commandService.configGet(configs)
    } yield {
      val filter = setupConfigList.find(_.prefix == "tmt.mobie.blue.filter").map(_("value").elems.head.toString)
      val disperser = setupConfigList.find(_.prefix == "tmt.mobie.blue.disperser").map(_("value").elems.head.toString)
      val demoData = DemoData(filter, disperser)
      val msg = WebSocketMessage(data = Some(demoData))
      val json = write(msg)
      wsChannel.push(json)
    }

    f.onFailure {
      case ex ⇒
        logger.error("Error in configGet", ex)
    }

    // Error checking? (Execution errors will be sent by websocket later)
    Ok("")
  }

}
