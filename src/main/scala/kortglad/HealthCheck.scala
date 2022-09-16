package kortglad

import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicBoolean

class HealthCheck(val healthy: AtomicBoolean = AtomicBoolean(true)) {
  val logger = LoggerFactory.getLogger("HealthCheck")
  def isHealthy = healthy.get()

  def killMe() =
    logger.error("Kill signal received, flipping healthy flag")
    healthy.set(false)

}
