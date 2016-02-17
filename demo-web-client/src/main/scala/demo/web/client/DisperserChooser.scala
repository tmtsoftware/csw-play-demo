package demo.web.client

/**
 * Manages the disperser combobox
 */
object DisperserChooser {

  /**
   * Available dispersers (hard coded for this demo)
   */
  val dispersers = List(
    "Mirror",
    "B1200_G5301",
    "R831_G5302",
    "B600_G5303",
    "B600_G5307",
    "R600_G5304",
    "R400_G5305",
    "R150_G5306"
  )
}

/**
 * Manages the disperser combobox
 * @param listener notified when the user makes a selection
 */
case class DisperserChooser(listener: String ⇒ Unit) extends FormComboBox("Disperser", DisperserChooser.dispersers, listener)