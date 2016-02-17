package demo.web.shared

/**
 * Simplified version of csw CommandStatus that is shared between scala and scala.js
 *
 * @param name status name
 * @param isDone true if command completed or there was an error (i.e.: not in progress)
 * @param isError true if there was an error
 * @param message error message, or empty
 */
case class SharedCommandStatus(name: String, isDone: Boolean = true, isError: Boolean = false, message: String = "")
