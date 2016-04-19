package controllers

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import csw.services.ccs.AssemblyController
import csw.services.kvs.{KvsSettings, StateVariableStore, TelemetrySubscriber}
import csw.services.loc._
import csw.util.cfg.Configurations.{ConfigInfo, SetupConfig, SetupConfigArg}
import csw.util.cfg.Events.StatusEvent
import csw.util.cfg.StandardKeys
import demo.web.shared.{DemoData, SharedCommandStatus, WebSocketMessage}
import csw.services.ccs.CommandStatus
import csw.services.loc.ComponentType.Assembly
import csw.services.loc.Connection.AkkaConnection
import csw.services.loc.LocationService.ResolvedAkkaLocation

/**
 * Defines props and messages received by the companion class
 */
object ApplicationActor {
  def props(wsActor: ActorRef) = Props(classOf[ApplicationActor], wsActor)
}

/**
 * An actor that implements the play server
 *
 * @param wsActor actor used to send messages to the websocket
 */
class ApplicationActor(wsActor: ActorRef) extends Actor with ActorLogging with LocationTrackerClientActor {

  import demo.web.shared.{WebSocketMessage, WebSocketRequest}
  import context.dispatcher

  val assemblyName = "Assembly-1"
  val componentId = ComponentId(assemblyName, Assembly)
  val connection = AkkaConnection(componentId)
  trackConnection(connection)

  // Subscribe to telemetry to get the simulated values of the filter/disperser wheel moving
  // and forward that info to the web app via web socket
  val telemetrySubscriber = context.actorOf(TelemetrySubscriberActor.props(wsActor))

  override def receive: Receive = trackerClientReceive() orElse {
    case msg: String ⇒ handleWebSocketRequest(msg)
    case x           ⇒ log.error(s"Received unexpected message: $x")
  }

  // Handles an incoming websocket request
  def handleWebSocketRequest(msg: String): Unit = {
    import upickle.default._
    try {
      val request = read[WebSocketRequest](msg)
      request.submit.foreach {
        case DemoData(filterOpt, disperserOpt) ⇒
          getLocation(connection).collect {
            case r @ ResolvedAkkaLocation(_, _, _, actorRefOpt) ⇒
              submit(actorRefOpt.get, filterOpt, disperserOpt)
            case x ⇒ log.warning(s"Can't submit since assembly location is not resolved: $x")
          }
      }
      request.configGet.foreach(_ ⇒ configGet())
    } catch {
      case ex: Exception ⇒ log.error("Invalid message received: $msg", ex)
    }
  }

  /**
   * Submits a config based on the given arguments to the assembly and returns the future command status.
   *
   * @param assembly     the target assembly
   * @param filterOpt    optional filter setting
   * @param disperserOpt optional disperser setting
   */
  def submit(assembly: ActorRef, filterOpt: Option[String], disperserOpt: Option[String]): Unit = {
    // XXX TODO - should be passed in from outside
    val obsId = "obs0001"
    val configInfo = ConfigInfo(obsId)

    val filterConfig = filterOpt.map(f ⇒ SetupConfig(StandardKeys.filterPrefix).set(StandardKeys.filter, f))
    val disperserConfig = disperserOpt.map(d ⇒ SetupConfig(StandardKeys.disperserPrefix).set(StandardKeys.disperser, d))
    val configs = List(filterConfig, disperserConfig).flatten
    if (configs.nonEmpty) {
      val setupConfigArg = SetupConfigArg(configInfo, configs: _*)
      context.actorOf(WsReplyActor.props(wsActor, assembly, setupConfigArg))
    }
  }

  /**
   * Sends the assembly's current configuration to the replyTo actor
   */
  def configGet(): Unit = {
    import upickle.default._
    val settings = KvsSettings(context.system)
    val svs = StateVariableStore(settings)
    for {
      filterOpt ← svs.get(StandardKeys.filterPrefix)
      disperserOpt ← svs.get(StandardKeys.disperserPrefix)
    } yield {
      val filter = filterOpt.flatMap(_.get(StandardKeys.filter))
      val disperser = disperserOpt.flatMap(_.get(StandardKeys.disperser))
      val demoData = DemoData(filter, disperser)
      val msg = WebSocketMessage(data = Some(demoData))
      val json = write(msg)
      wsActor ! json
    }
  }
}

// An actor that submits a config to and assembly and then forwards assembly status values to the client via websocket
// (using wsActor)
object WsReplyActor {
  def props(wsActor: ActorRef, assembly: ActorRef, setupConfigArg: SetupConfigArg) =
    Props(classOf[WsReplyActor], wsActor, assembly, setupConfigArg)

  // Converts a CommandStatus received from the assembly to a SharedCommandStatus that can be sent to the web app
  def statusToSharedStatus(s: CommandStatus): SharedCommandStatus = {
    SharedCommandStatus(s.name, s.isDone, s.isFailed, s.message)
  }
}

/**
 * An actor that submits a config to an assembly and then forwards assembly status values to the client via websocket
 *
 * @param wsActor        actor managing websocket to client
 * @param assembly       target assembly
 * @param setupConfigArg the config to submit to the assembly
 */
class WsReplyActor(wsActor: ActorRef, assembly: ActorRef, setupConfigArg: SetupConfigArg) extends Actor with ActorLogging {

  import WsReplyActor._
  import upickle.default._

  assembly ! AssemblyController.Submit(setupConfigArg)

  override def receive: Receive = {
    case status: CommandStatus ⇒
      log.info(s"Replying with command status: $status to websocket")
      val msg = WebSocketMessage(commandStatus = Some(statusToSharedStatus(status)))
      val json = write(msg)
      wsActor ! json
      if (status.isDone) context.stop(self)

    case x ⇒ log.error(s"Received unexpected message: $x")
  }
}

// An actor that subscribes to filter and disperser telemetry values and forwards them to the client via websocket
object TelemetrySubscriberActor {
  def props(wsActor: ActorRef) =
    Props(classOf[TelemetrySubscriberActor], wsActor)
}

/**
 * An actor that subscribes to filter and disperser telemetry values and forwards them to the client via websocket
 *
 * @param wsActor Websocket to client
 */
class TelemetrySubscriberActor(wsActor: ActorRef) extends TelemetrySubscriber {

  import upickle.default._

  // Subscribe to telemetry for filter and disperser
  subscribe(StandardKeys.filterPrefix, StandardKeys.disperserPrefix)

  override def receive: Receive = {
    case event: StatusEvent ⇒
      log.info(s"Received status event: $event")
      val msg = if (event.prefix == StandardKeys.filterPrefix)
        WebSocketMessage(currentFilterPos = event.get(StandardKeys.filter))
      else
        WebSocketMessage(currentDisperserPos = event.get(StandardKeys.disperser))
      val json = write(msg)
      log.info(s"Sending websocket message: $json")
      wsActor ! json

    case x ⇒ log.error(s"Received unexpected message: $x")
  }
}

