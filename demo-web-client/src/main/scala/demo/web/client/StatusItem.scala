package demo.web.client

import org.scalajs.dom
import org.scalajs.dom._

/**
 * Displays the status of a submitted command
 */
case class StatusItem(idStr: String = "status") extends Displayable {

  override def markup(): Element = {
    import scalatags.JsDom.all._
    h5("Status")(
      span(style := "margin-left:15px;"),
      span(id := "busyStatus", cls := "glyphicon glyphicon-refresh glyphicon-refresh-animate hide"),
      span(style := "margin-left:15px;"),
      span(id := idStr, cls := "label", " ")).render
  }
}
