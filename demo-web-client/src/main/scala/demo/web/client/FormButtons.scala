package demo.web.client

import org.scalajs.dom._

/**
 * Manages form buttons
 */
case class FormButtons() extends Displayable {
  val buttons = {
    import scalatags.JsDom.all._
    import scalacss.ScalatagsCss._
    div(Styles.formButtons).render
  }

  def addButton(labelStr: String, listener: () => Unit, buttonType: String = "submit"): Unit = {
    import scalatags.JsDom.all._
    import scalacss.ScalatagsCss._
    val styleObj = if (buttonType == "submit") Styles.formButtonPrimary else Styles.formButtonDefault
    buttons.appendChild(
      button(styleObj, `type` := "button", onclick := listener)(labelStr).render
    )
  }

  // HTML markup displaying the label and combobox
  override def markup(): Element = {
    import scalatags.JsDom.all._
    div(cls := "form-group")(buttons).render
  }
}
