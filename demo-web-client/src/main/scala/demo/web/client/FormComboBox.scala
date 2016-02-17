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

  val editedIcon = "glyphicon-asterisk"
  val savedIcon = "glyphicon-ok"
  val errorIcon = "glyphicon-remove"

  def stateItem = $(s"#$stateIdStr")

  // Put a check mark, * or X to the right of the select item to indicate the state (saved, edited, error)
  private val selectState = {
    import scalatags.JsDom.all._
    import scalacss.ScalatagsCss._
    span(Styles.comboboxState, id := stateIdStr, "aria-hidden".attr := "true").render
  }

  // The combobox
  private val selectItem = {
    import scalatags.JsDom.all._
    select(id := idStr, cls := "form-control", onchange := itemSelected _)(
      choices.map(s ⇒ option(value := s)(s))
    ).render
  }

  // called when an item is selected
  private def itemSelected(e: dom.Event): Unit = {
    stateItem.removeClass(s"$savedIcon $errorIcon text-danger text-success")
    stateItem.addClass(editedIcon)
    stateItem.removeClass("hidden")
    listener(getSelectedItem)
  }

  /**
   * Mark the item as successfully saved
   */
  def itemSaved(): Unit = {
    stateItem.removeClass(s"$editedIcon $errorIcon text-danger")
    stateItem.addClass(s"$savedIcon text-success")
    stateItem.removeClass("hidden")
  }

  /**
   * Mark the item as in error
   */
  def itemError(): Unit = {
    stateItem.removeClass(s"$editedIcon $savedIcon text-success")
    stateItem.addClass(s"$errorIcon text-danger")
    stateItem.removeClass("hidden")
  }

  // HTML markup displaying the label and combobox
  override def markup(): Element = {
    import scalatags.JsDom.all._
    import scalacss.ScalatagsCss._
    div(cls := "form-group")(
      label(Styles.comboboxLabel, `for` := idStr)(labelStr),
      div(cls := "col-sm-10 input-group")(selectItem, " ", selectState)
    ).render
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
