package demo.web.client

import org.scalajs.dom._

import scalacss.ScalatagsCss._
import scalatags.JsDom.all._

/**
 * Manages the main layout
 */
case class Layout(titleStr: String) extends Displayable {

  val panelBody = div(cls := "panel-body").render

  val layout =
    div(cls := "container")(
      div(Styles.layoutPanel)(
        div(cls := "panel-heading")(titleStr),
        panelBody)).render

  override def markup(): Element = layout

  /**
   * Adds an item to the layout.
   * @param displayable the item to be added
   */
  def addItem(displayable: Displayable): Unit = {
    panelBody.appendChild(displayable.markup())
  }

  /**
   * Adds an item to the layout.
   * @param e the element to be added
   */
  def addElement(e: Element): Unit = {
    panelBody.appendChild(e)
  }
}
