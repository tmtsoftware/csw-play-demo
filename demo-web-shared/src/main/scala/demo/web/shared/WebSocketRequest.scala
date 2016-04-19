package demo.web.shared

/**
 * Message sent (as JSON) from the web client to the server to make a request
 *
 * @param submit    if defined, submit the values for filter and/or disperser
 * @param configGet if defined, reply with the current values
 */
case class WebSocketRequest(submit: Option[DemoData] = None, configGet: Option[Unit] = None)

