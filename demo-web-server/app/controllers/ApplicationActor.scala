package controllers

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import csw.pkgDemo.hcd2.Hcd2
import csw.services.ccs.AssemblyController
import csw.services.ccs.CommandStatus.{CommandResponse, _}
import csw.services.loc._
import csw.util.param.Parameters.{CommandInfo, Setup}
import demo.web.shared.{DemoData, SharedCommandStatus, WebSocketMessage}
import csw.services.loc.ComponentType.Assembly
import csw.services.loc.Connection.AkkaConnection
import csw.services.loc.LocationService.{Location, ResolvedAkkaLocation}
import csw.util.akka.PublisherActor
import csw.util.param.StateVariable._

/**
 * Defines props and messages received by the companion class
 */
object ApplicationActor {
  def props(wsActor: ActorRef) = Props(new ApplicationActor(wsActor))

  // An actor that submits a setup to an assembly and then forwards the assembly status value to the client via websocket
  // (using wsActor)
  object WsReplyActor {
    def props(wsActor: ActorRef, assembly: ActorRef, setups: List[Setup]) =
      Props(new WsReplyActor(wsActor, assembly, setups))

    // Converts a CommandStatus received from the assembly to a SharedCommandStatus that can be sent to the web app
    def commandResponseToSharedCommandStatus(cr: CommandResponse): SharedCommandStatus = {
      val isDone = cr match {
        case Accepted | _: InProgress => false
        case _                        => true
      }
      val isFailed = cr match {
        case _: Invalid | _: NoLongerValid | _: Error => true
        case _                                        => false
      }
      val msg = cr match {
        case Error(m)             => Some(m)
        case Invalid(issue)       => Some(issue.reason)
        case NoLongerValid(issue) => Some(issue.reason)
        case _                    => None
      }
      SharedCommandStatus(cr.toString, isDone, isFailed, msg.getOrElse(""))
    }
  }

  /**
   * An actor that submits a config to an assembly and then forwards assembly status values to the client via websocket
   *
   * @param wsActor  actor managing websocket to client
   * @param assembly target assembly
   * @param setups   the setups to submit to the assembly
   */
  class WsReplyActor(wsActor: ActorRef, assembly: ActorRef, setups: List[Setup]) extends Actor with ActorLogging {

    import WsReplyActor._
    import upickle.default._

    setups.foreach(assembly ! AssemblyController.Submit(_))

    override def receive: Receive = receiveResponses(Nil)

    // Once all responses are in, send a websocket response to the webapp
    // (XXX This needs work to properly update the status item: Somewhat broken after refactoring...)
    def receiveResponses(statusList: List[SharedCommandStatus]): Receive = {
      case cr: CommandResponse =>
        val status = commandResponseToSharedCommandStatus(cr)
        if (status.isDone) {
          val newStatusList = status :: statusList
          val done = newStatusList.size == setups.size && !newStatusList.exists(!_.isDone)
          log.info(s"Received command response: $cr")
          if (done) {
            val msg = WebSocketMessage(commandStatus = Some(status))
            val json = write(msg)
            wsActor ! json
            context.stop(self)
          } else {
            context.become(receiveResponses(newStatusList))
          }
        }

      case x => log.error(s"Received unexpected message: $x")
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
      case ResolvedAkkaLocation(_, _, _, actorRefOpt) => actorRefOpt
    }
    val set = x.flatten
    set.foreach(_ ! PublisherActor.Subscribe)
  }

  // Receive actor messages
  override def receive: Receive = trackerClientReceive orElse {
    // A status update from the assembly
    case s: CurrentStates => handleStatusUpdate(s)

    // A web socket message from the client
    case msg: String      => handleWebSocketRequest(msg)

    case x                => log.error(s"Received unexpected message: $x from ${sender()}")
  }

  private def handleStatusUpdate(currentStates: CurrentStates): Unit = {
    import upickle.default._

    currentStates.states.foreach { s =>
      log.info(s"Received status update from assembly: $s")
      val (msg: WebSocketMessage, data: DemoData) = if (s.prefix.prefix == Hcd2.filterPrefix) {
        val filter = s.get(Hcd2.filterKey).map(_.head)
        (WebSocketMessage(currentFilterPos = filter), DemoData(filterOpt = filter, disperserOpt = currentData.disperserOpt))
      } else {
        val disperser = s.get(Hcd2.disperserKey).map(_.head)
        (WebSocketMessage(currentDisperserPos = disperser), DemoData(filterOpt = currentData.filterOpt, disperserOpt = disperser))
      }
      currentData = data
      val json = write(msg)
      log.info(s"Sending websocket message: $json")
      wsActor ! json
    }
  }

  // Handles an incoming websocket request
  def handleWebSocketRequest(msg: String): Unit = {
    import upickle.default._
    try {
      val request = read[WebSocketRequest](msg)
      request.submit.foreach {
        case DemoData(filterOpt, disperserOpt) =>
          getLocation(connection).collect {
            case ResolvedAkkaLocation(_, _, _, actorRefOpt) =>
              submit(actorRefOpt.get, filterOpt, disperserOpt)
            case x => log.warning(s"Can't submit since assembly location is not resolved: $x")
          }
      }
      request.configGet.foreach(_ => configGet())
    } catch {
      case ex: Exception => log.error("Invalid message received: $msg", ex)
    }
  }

  /**
   * Submits a setup based on the given arguments to the assembly and returns the future command status.
   *
   * @param assembly     the target assembly
   * @param filterOpt    optional filter setting
   * @param disperserOpt optional disperser setting
   */
  def submit(assembly: ActorRef, filterOpt: Option[String], disperserOpt: Option[String]): Unit = {
    import csw.util.param.ParameterSetDsl._
    // XXX TODO - should be passed in from outside
    val info = new CommandInfo("obs0001")

    val filterConfig = filterOpt.map(f => setup(info, Hcd2.filterPrefix, Hcd2.filterKey -> f))
    val disperserConfig = disperserOpt.map(d => setup(info, Hcd2.disperserPrefix, Hcd2.disperserKey -> d))
    val setups = List(filterConfig, disperserConfig).flatten
    if (setups.nonEmpty) {
      context.actorOf(ApplicationActor.WsReplyActor.props(wsActor, assembly, setups))
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

