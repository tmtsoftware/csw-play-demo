package demo.web.client

import demo.web.shared.{DemoData, SharedCommandStatus, WebSocketMessage, WebSocketRequest}
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.raw.HTMLStyleElement

import scala.scalajs.js
import scala.scalajs.js.{JSApp, timers}
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.TypedTag
import scalacss.Defaults._
import scalacss.ScalatagsCss._

/**
 * Main class for the demo web app.
 *
 * @param wsBaseUrl web socket base URL
 */
case class DemoWebClient(wsBaseUrl: String) {

  private val head = dom.document.head
  private val body = dom.document.body

  // Page components
  private val layout = Layout("CSW Play Demo")
  private val form = Form(submitForm)
  private val filterChooser = FilterChooser(filterSelected)
  private val filterTelemetry = TelemetryItem("filter", FilterChooser.filters)
  private val disperserChooser = DisperserChooser(disperserSelected)
  private val disperserTelemetry = TelemetryItem("disperser", DisperserChooser.dispersers)
  private val buttons = FormButtons()
  private val statusItem = StatusItem()
  private def divider = {
    import scalatags.JsDom.all._
    hr().render
  }

  doLayout()

  // WebSocket to server
  private val websocket = initWebSocket()

  statusItem.clearStatus()

  // Display the current values on start (Safari and Firefox seem to need the delay)
  timers.setTimeout(500) {
    refreshButtonSelected()
  }

  // Layout the components on the page
  private def doLayout(): Unit = {
    // Add CSS styles
    head.appendChild(Styles.render[TypedTag[HTMLStyleElement]].render)

    form.addItem(filterChooser)
    form.addItem(filterTelemetry)
    form.addItem(disperserChooser)
    form.addItem(disperserTelemetry)

    buttons.addButton("Refresh", refreshButtonSelected, "button")
    buttons.addButton("Apply", applyButtonSelected, "submit")
    form.addItem(buttons)

    layout.addItem(form)
    layout.addElement(divider)
    layout.addItem(statusItem)

    //    layout.addElement(divider)
    //    layout.addItem(telemetryItem)

    body.appendChild(layout.markup())
  }

  // Initialize a websocket for status messages on a running submit
  private def initWebSocket(): dom.WebSocket = {
    val socket = new dom.WebSocket(wsBaseUrl)
    socket.onmessage = wsReceive _
    socket
  }

  // Receive a status message from the server websocket
  private def wsReceive(e: dom.MessageEvent): Unit = {
    import upickle.default._
    println(s"XXX wsReceive ${e.data.toString}")
    val wsMsg = read[WebSocketMessage](e.data.toString)
    wsMsg.commandStatus foreach setCommandStatus
    wsMsg.data foreach setData
    wsMsg.currentFilterPos foreach setCurrentFilterPos
    wsMsg.currentDisperserPos foreach setCurrentDisperserPos
  }

  // Displays the return status from a submit command
  private def setCommandStatus(status: SharedCommandStatus): Unit = {
    statusItem.setStatus(status)
    if (status.isError) {
      filterChooser.itemError()
      disperserChooser.itemError()
    } else if (status.isDone) {
      filterChooser.itemSaved()
      disperserChooser.itemSaved()
    }
  }

  // Displays data returned from a configGet request
  private def setData(data: DemoData): Unit = {
    data.filterOpt.foreach(f => filterChooser.setSelectedItem(f, notifyListener = false))
    data.disperserOpt.foreach(d => disperserChooser.setSelectedItem(d, notifyListener = false))
  }

  // Displays the current filter position (from telemetry)
  private def setCurrentFilterPos(filterPos: String): Unit = {
    println(s"XXX set current filter pos to $filterPos")
    filterTelemetry.setPos(filterPos)
  }

  // Displays the current disperser position (from telemetry)
  private def setCurrentDisperserPos(disperserPos: String): Unit = {
    println(s"XXX set current disperser pos to $disperserPos")
    disperserTelemetry.setPos(disperserPos)
  }

  // Called when a new filter was selected
  private def filterSelected(filter: String): Unit = {
  }

  // Called when a new disperser was selected
  private def disperserSelected(disperser: String): Unit = {
  }

  private def refreshButtonSelected(): Unit = {
    import upickle.default._
    val request = WebSocketRequest(configGet = Some(()))
    val json = write(request)
    websocket.send(json)
  }

  private def applyButtonSelected(): Unit = {
    import upickle.default._
    val filter = filterChooser.getSelectedItem
    val disperser = disperserChooser.getSelectedItem
    val request = WebSocketRequest(submit = Some(DemoData(filterOpt = Some(filter), disperserOpt = Some(disperser))))
    val json = write(request)
    websocket.send(json)
  }

  private def submitForm(e: Event): Unit = {
    e.preventDefault()
    applyButtonSelected()
  }
}

/**
 * Main entry object for the web app
 */
object DemoWebClient extends JSApp {

  /**
   * Main entry point from Play
   * @param settings a JavaScript object containing settings (see class DemoWebClient)
   * @return
   */
  @JSExport
  def init(settings: js.Dynamic) = {
    val wsBaseUrl = settings.wsBaseUrl.toString
    DemoWebClient(wsBaseUrl)
  }

  // Main entry point (not used, see init() above)
  @JSExport
  override def main(): Unit = {
  }
}
