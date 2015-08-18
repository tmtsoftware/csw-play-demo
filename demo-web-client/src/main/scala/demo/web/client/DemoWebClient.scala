package demo.web.client

//import demo.web.shared._

import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw.HTMLStyleElement
import upickle.default._
import org.querki.jquery._

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.TypedTag
import scalacss.Defaults._
import scalacss.ScalatagsCss._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Main class for the demo web app.
 *
 * @param csrfToken server token used for security
 * @param wsBaseUrl web socket base URL
 */
case class DemoWebClient(csrfToken: String, wsBaseUrl: String) {

  private val head = dom.document.head
  private val body = dom.document.body

  // Page components
  private val layout = Layout("CSW Play Demo")
  private val form = Form(submitForm)
  private val filterChooser = FilterChooser(filterSelected)
  private val disperserChooser = DisperserChooser(disperserSelected)
  private val buttons = FormButtons()
  //  private val progressBar = ProgressBar()
  private val statusItem = StatusItem()
  private def divider = {
    import scalatags.JsDom.all._
    hr().render
  }

  doLayout()
  initWebSocket()
  statusItem.clearStatus()

  // Layout the components on the page
  private def doLayout(): Unit = {
    // Add CSS styles
    head.appendChild(Styles.render[TypedTag[HTMLStyleElement]].render)

    form.addItem(filterChooser)
    form.addItem(disperserChooser)

    //    form.addElement(divider)

    buttons.addButton("Refresh", refreshButtonSelected, "button")
    buttons.addButton("Apply", applyButtonSelected, "submit")
    form.addItem(buttons)

    layout.addItem(form)
    layout.addElement(divider)
    //    layout.addItem(progressBar)
    layout.addItem(statusItem)

    body.appendChild(layout.markup())
  }

  // Initialize a websocket for status messages on a running submit
  private def initWebSocket(): Unit = {
    val socket = new dom.WebSocket(wsBaseUrl)
    socket.onmessage = wsReceive _
  }

  // Receive a status message from the server websocket
  private def wsReceive(e: dom.MessageEvent): Unit = {
    val statusStr = e.data.toString
    statusItem.setStatus(statusStr)
    // XXX TODO How to get status of single item?
    statusStr match {
      case "completed" ⇒
        filterChooser.itemSaved()
        disperserChooser.itemSaved()
      case "error" ⇒
        filterChooser.itemError()
        disperserChooser.itemError()

      case _ ⇒
    }
  }

  // Called when a new filter was selected
  private def filterSelected(filter: String): Unit = {
  }

  // Called when a new disperser was selected
  private def disperserSelected(disperser: String): Unit = {
  }

  private def refreshButtonSelected(): Unit = {
  }

  private def applyButtonSelected(): Unit = {
    val filter = filterChooser.getSelectedItem
    val disperser = disperserChooser.getSelectedItem

    //    progressBar.setActive(true)

    // XXX FIXME
    val url = Routes.submit(filter, disperser)
    Ajax.post(url).map { r ⇒
      // XXX TODO Check result status
    }

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
    val csrfToken = settings.csrfToken.toString
    DemoWebClient(csrfToken, wsBaseUrl)
  }

  // Main entry point (not used, see init() above)
  @JSExport
  override def main(): Unit = {
  }
}