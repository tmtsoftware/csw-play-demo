package demo.web.client

import org.scalajs.dom._

/**
 * An empty form that you can add items to
 */
case class Form(handler: Event => Unit) extends Displayable {
  val formItem = {
    import scalatags.JsDom.all._
    import scalacss.ScalatagsCss._
    form(Styles.form, onsubmit := handler _).render
  }

  override def markup(): Element = formItem

  /**
   * Adds an item to the form.
   * @param displayable the item to be added
   */
  def addItem(displayable: Displayable): Unit = {
    formItem.appendChild(displayable.markup())
  }

  /**
   * Adds an element to the form.
   * @param e the element to be added
   */
  def addElement(e: Element): Unit = {
    formItem.appendChild(e)
  }
}
