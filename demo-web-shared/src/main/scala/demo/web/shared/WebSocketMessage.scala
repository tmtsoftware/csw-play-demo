package demo.web.shared

/**
 * Type of a websocket message sent from the play server to the web client.
 * Different optional fields are set based on the type of message being sent.
 *
 * @param commandStatus the status of a submitted command
 * @param data data requested with configGet (current state)
 */
case class WebSocketMessage(
  commandStatus:       Option[SharedCommandStatus] = None,
  currentFilterPos:    Option[String]              = None,
  currentDisperserPos: Option[String]              = None,
  data:                Option[DemoData]            = None
)
