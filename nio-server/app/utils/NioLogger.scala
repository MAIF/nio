package utils
import play.api.{MarkerContext, Logger => PlayLogger}

object NioLogger {

  val logger = PlayLogger("nio")

  def debug(message: => String)(implicit mc: MarkerContext): Unit =
    logger.debug(message)(mc)

  def debug(message: => String, error: => Throwable)(implicit mc: MarkerContext): Unit =
    logger.debug(message, error)(mc)

  def info(message: => String)(implicit mc: MarkerContext): Unit =
    logger.info(message)(mc)

  def warn(message: => String)(implicit mc: MarkerContext): Unit =
    logger.warn(message)(mc)

  def warn(message: => String, error: => Throwable)(implicit mc: MarkerContext): Unit =
    logger.warn(message, error)(mc)

  def error(message: => String)(implicit mc: MarkerContext): Unit =
    logger.error(message)(mc)

  def error(message: => String, error: => Throwable)(implicit mc: MarkerContext): Unit =
    logger.error(message, error)(mc)

}
