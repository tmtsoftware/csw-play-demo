package demo.web.client

import demo.web.shared.SharedCommandStatus
import org.querki.jquery._
import org.scalajs.dom._

/**
 * Displays the status of a submitted command
 */
case class StatusItem(idStr: String = "status") extends Displayable {

  override def markup(): Element = {
    import scalatags.JsDom.all._
    h5("Status")(
      span(style := "margin-left:15px;"),
      span(id := s"busy$idStr", cls := "glyphicon glyphicon-refresh"),
      span(style := "margin-left:15px;"),
      span(id := idStr, cls := "label", " ")
    ).render
  }

  private def statusItem = $(s"#$idStr")

  def busyStatusItem = $(s"#busy$idStr")

  /**
   * Sets the command status to display
   */
  def setStatus(status: SharedCommandStatus): Unit = {
    val labelClass = if (status.isError) "label-danger" else if (status.isDone) "label-success" else "label-info"

    statusItem
      .removeClass("label-success label-danger label-info label-default")
      .addClass(labelClass)
      .text(status.name)

    if (status.isDone)
      busyStatusItem.hide().removeClass("glyphicon-refresh-animate")
    else
      busyStatusItem.show().addClass("glyphicon-refresh-animate")
  }

  /**
   * Resets the status display
   */
  def clearStatus(): Unit = {
    statusItem
      .removeClass("label-success label-danger label-info")
      .addClass("label-default")
      .text("")
    busyStatusItem.hide().removeClass("glyphicon-refresh-animate")
  }
}
