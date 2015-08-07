package demo.web.client

import org.scalajs.dom
import org.scalajs.dom._
import org.querki.jquery._

/**
 * Simple ComboBox for a form class that displays a list of strings and a glyph indicating
 * if the item has changed or was successfully applied.
 */
class FormComboBox(labelStr: String, choices: List[String], listener: String ⇒ Unit) extends Displayable {

  val idStr = labelStr.toLowerCase
  val stateIdStr = idStr + "State"

  // Put a check mark or X to the righ of the select item to indicate the state
  private val selectState = {
    import scalatags.JsDom.all._
    import scalacss.ScalatagsCss._
    span(Styles.comboboxState, id := stateIdStr, "aria-hidden".attr := "true").render
  }

  // The combobox
  private val selectItem = {
    import scalatags.JsDom.all._
    select(id := idStr, cls := "form-control", onchange := itemSelected _)(
      choices.map(s ⇒ option(value := s)(s))).render
  }

  // called when an item is selected
  private def itemSelected(e: dom.Event): Unit = {
    val stateItem = $(s"#$stateIdStr")
    stateItem.removeClass("glyphicon-ok")
    stateItem.addClass("glyphicon-remove")
    stateItem.removeClass("hidden")
    listener(getSelectedItem)
  }

  /**
   * Mark the item as successfully saved
   */
  def itemSaved(): Unit = {
    val stateItem = $(s"#$stateIdStr")
    stateItem.removeClass("glyphicon-remove")
    stateItem.addClass("glyphicon-ok")
    stateItem.removeClass("hidden")
  }

  // HTML markup displaying the label and combobox
  override def markup(): Element = {
    import scalatags.JsDom.all._
    import scalacss.ScalatagsCss._
    div(cls := "form-group")(
      label(Styles.comboboxLabel, `for` := idStr)(labelStr),
      div(cls := "col-sm-10 input-group")(selectItem, " ", selectState)).render
  }

  /**
   * Gets the currently selected item
   */
  def getSelectedItem: String = selectItem.value

  /**
   * Sets the selected item.
   */
  def setSelectedItem(item: String, notifyListener: Boolean = true): Unit = {
    if (item != getSelectedItem) {
      selectItem.value = item
    }
  }

}
