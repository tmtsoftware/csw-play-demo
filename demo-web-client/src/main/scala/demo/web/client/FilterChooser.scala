package demo.web.client

/**
 * Manages the filter combobox
 */
object FilterChooser {

  /**
   * Available filters (hard coded for this demo)
   */
  val filters = List(
    "None",
    "g_G0301",
    "r_G0303",
    "i_G0302",
    "z_G0304",
    "Z_G0322",
    "Y_G0323",
    "u_G0308"
  )
}

/**
 * Manages the filter combobox
 * @param listener notified when the user makes a selection
 */
case class FilterChooser(listener: String ⇒ Unit) extends FormComboBox("Filter", FilterChooser.filters, listener)