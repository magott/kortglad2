package kortglad
import bloque.db.*
import org.slf4j.LoggerFactory

import java.util.concurrent.{Executors, TimeUnit}

object Jobs {

  lazy val executor = Executors.newSingleThreadScheduledExecutor()
  val logger = LoggerFactory.getLogger("Jobs")

  def setupSingleMatchScraperJob(db: Db) =
    executor.scheduleAtFixedRate(
      () => singleMatchScrapeJob(db),
      1,
      120,
      TimeUnit.SECONDS
    )

  def singleMatchScrapeJob(db: Db) =
    val next: Option[MatchJob] = db {
      readNextJob.option
    }
    logger.info(s"Matchjob triggered, will add $next")
    next.foreach { matchJob =>
      val count = RefereeService(db).addSingleMatch(matchJob.matchId)
      logger.info(s"$matchJob update count $count")
      db {
        markJobRead(matchJob.id).run
      }
    }

  def readNextJob =
    sql"""
        select id, match_id from match_scraper_job order by id limit 1   
    """.query[MatchJob]

  def markJobRead(jobId: Int) =
    sql"""
        delete match_scraper_job where id = $jobId
     """.update

  case class MatchJob(id: Int, matchId: FiksId) derives Row
}
