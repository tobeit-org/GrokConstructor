package net.stoerr.grokconstructor

/** Wraps a DeadlineExceededException for better error message. */
class TimeoutException(msg: String, e: Throwable) extends Exception(msg, e) {

  // empty

}
