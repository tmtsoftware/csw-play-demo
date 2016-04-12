package controllers

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import csw.services.ccs.AssemblyController
import csw.services.kvs.{KvsSettings, StateVariableStore, TelemetrySubscriber}
import csw.services.loc._
import csw.util.cfg.Configurations.{ConfigInfo, SetupConfig, SetupConfigArg}
import csw.util.cfg.Events.StatusEvent
import csw.util.cfg.StandardKeys
import demo.web.shared.{DemoData, SharedCommandStatus, WebSocketMessage}
import play.api.libs.iteratee.Concurrent.Channel
import csw.services.ccs.CommandStatus
import csw.services.loc.ComponentType.Assembly
import csw.services.loc.Connection.AkkaConnection
import csw.services.loc.LocationService.ResolvedAkkaLocation

/**
 * Defines props and messages received by the companion class
 */
object ApplicationActor {
  LocationService.initInterface()

  def props(wsChannel: Channel[String]) = Props(classOf[ApplicationActor], wsChannel)

  sealed trait Assembly1ActorMessage

  /**
   * Message to submit a config to the assembly
   *
   * @param filterOpt    optional filter value
   * @param disperserOpt optional disperser value
   */
  case class Submit(filterOpt: Option[String], disperserOpt: Option[String]) extends Assembly1ActorMessage

  /**
   * Message to get the current configuration from the assembly
   */
  case object ConfigGet

}

/**
 * An actor that implements the play server
 *
 * @param wsChannel web socket to use to send results to client
 */
class ApplicationActor(wsChannel: Channel[String]) extends Actor with ActorLogging with LocationTrackerClientActor {

  import context.dispatcher

  val assemblyName = "Assembly-1"
  val componentId = ComponentId(assemblyName, Assembly)
  val connection = AkkaConnection(componentId)
  trackConnection(connection)

  // Subscribe to telemetry to get the simulated values of the filter/disperser wheel moving
  // and forward that info to the web app via web socket
  val telemetrySubscriber = context.actorOf(TelemetrySubscriberActor.props(wsChannel))

  override def receive: Receive = trackerClientReceive orElse {
    case ApplicationActor.Submit(filterOpt, disperserOpt) ⇒
      getLocation(connection).collect {
        case r @ ResolvedAkkaLocation(_, _, _, actorRefOpt) ⇒
          submit(actorRefOpt.get, filterOpt, disperserOpt)
        case x ⇒ log.warning(s"Can't submit since assembly location is not resolved: $x")
      }

    case ApplicationActor.ConfigGet ⇒
      configGet()

    case x ⇒ log.error(s"Received unexpected message: $x")
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
      context.actorOf(WsReplyActor.props(wsChannel, assembly, setupConfigArg))
    }
  }

  /**
   * Sends the assembly's current configuration to the replyTo actor
   */
  def configGet(): Unit = {
    import upickle.default._
    // XXX TODO - should be passed in from outside
    val obsId = "obs0001"
    val configInfo = ConfigInfo(obsId)

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
      wsChannel.push(json)
    }
  }
}

// An actor that submits a config to and assembly and then forwards assembly status values to the client via websocket
object WsReplyActor {
  def props(wsChannel: Channel[String], assembly: ActorRef, setupConfigArg: SetupConfigArg) =
    Props(classOf[WsReplyActor], wsChannel, assembly, setupConfigArg)

  // Converts a CommandStatus received from the assembly to a SharedCommandStatus that can be sent to the web app
  def statusToSharedStatus(s: CommandStatus): SharedCommandStatus = {
    SharedCommandStatus(s.name, s.isDone, s.isFailed, s.message)
  }
}

/**
 * An actor that submits a config to and assembly and then forwards assembly status values to the client via websocket
 *
 * @param wsChannel      Websocket to client
 * @param assembly       target assembly
 * @param setupConfigArg the config to submit to the assembly
 */
class WsReplyActor(wsChannel: Channel[String], assembly: ActorRef, setupConfigArg: SetupConfigArg) extends Actor with ActorLogging {

  import WsReplyActor._
  import upickle.default._

  assembly ! AssemblyController.Submit(setupConfigArg)

  override def receive: Receive = {
    case status: CommandStatus ⇒
      log.info(s"Replying with command status: $status to wsChannel $wsChannel")
      val msg = WebSocketMessage(commandStatus = Some(statusToSharedStatus(status)))
      val json = write(msg)
      wsChannel.push(json)
      if (status.isDone) context.stop(self)

    case x ⇒ log.error(s"Received unexpected message: $x")
  }
}

// An actor that subscribes to filter and disperser telemetry values and forwards them to the client via websocket
object TelemetrySubscriberActor {
  def props(wsChannel: Channel[String]) =
    Props(classOf[TelemetrySubscriberActor], wsChannel)
}

/**
 * An actor that subscribes to filter and disperser telemetry values and forwards them to the client via websocket
 *
 * @param wsChannel Websocket to client
 */
class TelemetrySubscriberActor(wsChannel: Channel[String]) extends TelemetrySubscriber {

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
      wsChannel.push(json)

    case x ⇒ log.error(s"Received unexpected message: $x")
  }
}

