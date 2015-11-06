package controllers

import akka.actor.{ Props, ActorRef, ActorLogging, Actor }
import akka.util.Timeout
import csw.services.ccs.AssemblyController
import csw.services.kvs.{ StateVariableStore, KvsSettings }
import csw.services.loc.AccessType.AkkaType
import csw.services.loc.LocationService.{ Disconnected, ServicesReady }
import csw.services.loc.{ LocationService, ServiceType, ServiceId, ServiceRef }
import csw.util.cfg.Configurations.{ ConfigInfo, SetupConfigArg, SetupConfig }
import csw.util.cfg.StandardKeys
import demo.web.shared.{ DemoData, SharedCommandStatus, WebSocketMessage }
import play.api.libs.iteratee.Concurrent.Channel
import scala.concurrent.duration._
import scala.util.{ Failure, Success }
import csw.services.ccs.CommandStatus

/**
 * Defines props and messages received by the companion class
 */
object ApplicationActor {

  def props = Props[ApplicationActor]

  sealed trait Assembly1ActorMessage

  /**
   * Message to submit a config to the assembly
   * @param wsChannel websocket used to send the command status values to the client
   * @param filterOpt optional filter value
   * @param disperserOpt optional disperser value
   */
  case class Submit(wsChannel: Channel[String], filterOpt: Option[String], disperserOpt: Option[String]) extends Assembly1ActorMessage

  /**
   * Message to get the current configuration from the assembly
   */
  case class ConfigGet(wsChannel: Channel[String])
}

/**
 * An actor that implements the play server
 */
class ApplicationActor extends Actor with ActorLogging {

  import context.dispatcher

  // Locate the assembly
  val assemblyName = "Assembly-1"
  val serviceRef = ServiceRef(ServiceId(assemblyName, ServiceType.Assembly), AkkaType)
  context.actorOf(LocationService.props(Set(serviceRef), Some(self)))

  // Don't accept any messages until we have located the assembly through the location service
  override def receive: Receive = {
    case ServicesReady(services) ⇒ context.become(ready(services(serviceRef).actorRefOpt.get))

    case x                       ⇒ log.error(s"$assemblyName not ready to receive messages")
  }

  // Ready state, we have a reference to the assembly actor
  def ready(assembly: ActorRef): Receive = {
    case ApplicationActor.Submit(wsChannel, filterOpt, disperserOpt) ⇒
      submit(wsChannel, assembly, filterOpt, disperserOpt)

    case ApplicationActor.ConfigGet(wsChannel) ⇒
      configGet(wsChannel, sender())

    case Disconnected            ⇒ context.become(receive)

    case ServicesReady(services) ⇒ context.become(ready(services(serviceRef).actorRefOpt.get))

    case x                       ⇒ log.error(s"Received unexpected message: $x")
  }

  /**
   * Submits a config based on the given arguments to the assembly and returns the future command status.
   * @param wsChannel websocket used to send the command status values to the client
   * @param assembly the target assembly
   * @param filterOpt optional filter setting
   * @param disperserOpt optional disperser setting
   */
  def submit(wsChannel: Channel[String], assembly: ActorRef, filterOpt: Option[String], disperserOpt: Option[String]): Unit = {
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
  def configGet(wsChannel: Channel[String], replyTo: ActorRef): Unit = {
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

// An actor that forwards assembly status values to the client via websocket
object WsReplyActor {
  def props(wsChannel: Channel[String], assembly: ActorRef, setupConfigArg: SetupConfigArg) =
    Props(classOf[WsReplyActor], wsChannel, assembly, setupConfigArg)

  def statusToSharedStatus(s: CommandStatus): SharedCommandStatus = {
    SharedCommandStatus(s.name, s.isDone, s.isFailed, s.message)
  }
}

/**
 * An actor that forwards assembly status values to the client via websocket
 * @param wsChannel Websocket to client
 * @param assembly target assembly
 * @param setupConfigArg the config to submit to the assembly
 */
class WsReplyActor(wsChannel: Channel[String], assembly: ActorRef, setupConfigArg: SetupConfigArg) extends Actor with ActorLogging {
  import WsReplyActor._
  import upickle.default._
  assembly ! AssemblyController.Submit(setupConfigArg)
  override def receive: Receive = {
    case status: CommandStatus ⇒
      val msg = WebSocketMessage(status = Some(statusToSharedStatus(status)))
      val json = write(msg)
      wsChannel.push(json)
      if (status.isDone) context.stop(self)

    case x ⇒ log.error(s"Received unexpected message: $x")
  }
}

