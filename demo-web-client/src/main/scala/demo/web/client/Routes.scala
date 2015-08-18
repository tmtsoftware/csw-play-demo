package demo.web.client

/**
 * Defines URI routes to access the server API
 * (See demo-web-server/conf/routes file for server side)
 */
object Routes {
  def submit(filter: String, disperser: String): String = s"/submit?filter=$filter&disperser=$disperser"
}
