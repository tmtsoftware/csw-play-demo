package demo.web.client

import demo.web.shared.SharedCommandStatus
import org.querki.jquery._
import org.scalajs.dom._

/**
 * Displays telemetry for the filter and disperser (current values received via websocket)
 *
 * @param name filter or dispersr
 * @param choices the choices array
 */
case class TelemetryItem(name: String, choices: List[String]) extends Displayable {

  // Element ids
  val textIdStr = s"${name}Text"
  def itemIdStr(i: Int) = s"${name}Item$i"

  /**
   * Update the telemetry display to show the current position
   * @param pos one of the choices
   */
  def setPos(pos: String): Unit = {
    $(s"#$textIdStr").text(pos)
    for (i ← choices.indices)
      $(s"#${itemIdStr(i)}").removeClass("active").addClass("disabled")
    val i = choices.indexOf(pos)
    if (i >= 0 && i < choices.size) {
      $(s"#${itemIdStr(i)}").removeClass("disabled").addClass("active")
    }
  }

  override def markup(): Element = {
    import scalatags.JsDom.all._
    import scalacss.ScalatagsCss._
    div(cls := "form-group")(
      label(Styles.comboboxLabel)(" "),
      div(cls := "col-sm-10 input-group")(
        ul(cls := "pagination pagination-sm")(
          for (i ← choices.indices) yield {
            li(id := itemIdStr(i), cls := "disabled")(a(href := "#")(i + 1))
          },
          span(Styles.telemetryText, id := textIdStr)(" ")
        )
      )
    ).render
  }

}
