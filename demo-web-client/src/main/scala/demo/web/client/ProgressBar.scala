package demo.web.client

import org.scalajs.dom
import org.scalajs.dom._
import org.querki.jquery._

/**
 * Displays a progress bar
 */
case class ProgressBar(idStr: String = "progress") extends Displayable {

  private val progressBar = {
    import scalatags.JsDom.all._
    import scalacss.ScalatagsCss._
    div(Styles.progressBar, id := idStr, role := "progressbar",
      "aria-valuenow".attr := "0", "aria-valuemin".attr := "0",
      "aria-valuemax".attr := "100", style := "width: 0%", "0%").render
  }

  /**
   * Sets the progress bar to active/not active
   * @param active true if active
   */
  def setActive(active: Boolean): Unit = {
    val p = $(s"#$idStr")
    if (active)
      p.addClass("active")
    else
      p.removeClass("active")
  }

  /**
   * Sets the progress bar by percent
   * @param percent percent done
   */
  def setProgress(percent: Int): Unit = {
    val p = $(s"#$idStr")
    p.css("width", percent + "%").attr("aria-valuenow", percent.toString).html(s"$percent %")
  }

  override def markup(): Element = {
    import scalatags.JsDom.all._
    div(cls := "progress")(progressBar).render
  }
}
