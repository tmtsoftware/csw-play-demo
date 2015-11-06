package demo.web.shared

/**
 * Type of a websocket message sent from the play server to the web client
 *
 * @param status the status of a submitted command
 * @param data data requested with configGet (current state)
 */
case class WebSocketMessage(status: Option[SharedCommandStatus] = None, data: Option[DemoData] = None)
