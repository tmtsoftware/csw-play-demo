package controllers

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import csw.services.ccs.AssemblyController
import csw.services.loc._
import csw.util.cfg.Configurations.{ConfigInfo, SetupConfig, SetupConfigArg}
import csw.util.cfg.StandardKeys
import demo.web.shared.{DemoData, SharedCommandStatus, WebSocketMessage}
import csw.services.ccs.CommandStatus
import csw.services.loc.ComponentType.Assembly
import csw.services.loc.Connection.AkkaConnection
import csw.services.loc.LocationService.{Location, ResolvedAkkaLocation}
import csw.util.akka.PublisherActor
import csw.util.cfg.StateVariable.CurrentState

/**
 * Defines props and messages received by the companion class
 */
object ApplicationActor {
  def props(wsActor: ActorRef) = Props(classOf[ApplicationActor], wsActor)

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

}

/**
 * An actor that implements the play server
 *
 * @param wsActor actor used to send messages to the websocket
 */
class ApplicationActor(wsActor: ActorRef) extends Actor with ActorLogging with LocationTrackerClientActor {
  import demo.web.shared.{WebSocketMessage, WebSocketRequest}

  private val assemblyName = "Assembly-1"
  private val componentId = ComponentId(assemblyName, Assembly)
  private val connection = AkkaConnection(componentId)

  // Saved current values from assembly for use in answering a configGet query
  private var currentData: DemoData = DemoData()

  trackConnection(connection)

  //  // Subscribe to telemetry to get the simulated values of the filter/disperser wheel moving
  //  // and forward that info to the web app via web socket
  //  private val telemetrySubscriber = context.actorOf(TelemetrySubscriberActor.props(wsActor))

  // Called when the assembly location has been resolved: Subscribe to status values from the assembly
  override protected def allResolved(locations: Set[Location]): Unit = {
    val x = locations.collect {
      case r @ ResolvedAkkaLocation(_, _, _, actorRefOpt) ⇒ actorRefOpt
    }
    val set = x.flatten
    set.foreach(_ ! PublisherActor.Subscribe)
  }

  // Receive actor messages
  override def receive: Receive = trackerClientReceive orElse {
    // A status update from the assembly
    case s: CurrentState ⇒ handleStatusUpdate(s)

    // A web socket message from the client
    case msg: String     ⇒ handleWebSocketRequest(msg)

    case x               ⇒ log.error(s"Received unexpected message: $x")
  }

  private def handleStatusUpdate(s: CurrentState): Unit = {
    import upickle.default._

    log.info(s"Received status update from assembly: $s")
    val (msg: WebSocketMessage, data: DemoData) = if (s.prefix == StandardKeys.filterPrefix) {
      val filter = s.get(StandardKeys.filter)
      (WebSocketMessage(currentFilterPos = filter), DemoData(filterOpt = filter, disperserOpt = currentData.disperserOpt))
    } else {
      val disperser = s.get(StandardKeys.disperser)
      (WebSocketMessage(currentDisperserPos = disperser), DemoData(filterOpt = currentData.filterOpt, disperserOpt = disperser))
    }
    currentData = data
    val json = write(msg)
    log.info(s"Sending websocket message: $json")
    wsActor ! json
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
      context.actorOf(ApplicationActor.WsReplyActor.props(wsActor, assembly, setupConfigArg))
    }
  }

  /**
   * Sends the assembly's current configuration to the replyTo actor
   */
  def configGet(): Unit = {
    import upickle.default._
    val msg = WebSocketMessage(data = Some(currentData))
    val json = write(msg)
    wsActor ! json
  }
}

