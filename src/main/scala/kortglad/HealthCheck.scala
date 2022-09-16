package kortglad

import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

class HealthCheck(
    val healthy: AtomicBoolean = AtomicBoolean(true),
    val reason: AtomicReference[String] = AtomicReference()
) {
  val logger = LoggerFactory.getLogger("HealthCheck")
  def isHealthy = healthy.get()

  def killMe(reason: String = "Fotball.no connectivity issues") =
    logger.error("Kill signal received, flipping healthy flag")
    this.reason.set(reason)
    healthy.set(false)

}
