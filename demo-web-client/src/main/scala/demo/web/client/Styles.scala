package demo.web.client

import scalacss.Defaults._

// CSS styles
object Styles extends StyleSheet.Inline {

  import dsl._

  import language.postfixOps

  val layoutPanel = style(
    addClassNames("panel", "panel-default"),
    marginTop(100 px),
    minWidth(300 px),
    maxWidth(500 px)
  )

  val formButtonPrimary = style(
    addClassNames("btn", "btn-primary"),
    marginRight(15 px)
  )

  val formButtonDefault = style(
    addClassNames("btn", "btn-default"),
    marginRight(15 px)
  )

  val progressBar = style(
    addClassNames("progress-bar", "progress-bar-info", "progress-bar-striped")
  )

  val comboboxState = style(
    addClassNames("glyphicon", "glyphicon-ok", "input-group-addon", "hidden"),
    backgroundColor.transparent,
    border.none
  )

  val comboboxLabel = style(
    addClassNames("col-sm-2", "control-label"),
    paddingLeft(0 px)
  )

  val telemetryText = style(
    paddingLeft(10 px),
    verticalAlign.middle
  )

  val formButtons = style(
    addClassNames("col-sm-offset-2", "col-sm-10"),
    paddingLeft(0 px)
  )

  val form = style(
    addClassName("form-horizontal"),
    paddingLeft(10 px),
    paddingRight(10 px)
  )
}
