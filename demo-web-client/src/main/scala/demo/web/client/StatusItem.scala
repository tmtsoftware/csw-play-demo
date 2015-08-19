package demo.web.client

import csw.shared.CommandStatus
import csw.shared.CommandStatus._
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
      span(id := idStr, cls := "label", " ")).render
  }

  private def statusItem = $(s"#$idStr")

  def busyStatusItem = $(s"#busy$idStr")

  /**
   * Sets the command status to display
   * (From CommandStatus.toString: one of "queued", "busy", "partially completed", "completed", "error")
   */
  def setStatus(status: CommandStatus): Unit = {
    val labelClass = status match {
      case Completed(_) ⇒ "label-success"
      case Error(_, _)  ⇒ "label-danger"
      case _            ⇒ "label-info"
    }
    statusItem
      .removeClass("label-success label-danger label-info label-default")
      .addClass(labelClass)
      .text(status.name)

    if (status.done)
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
