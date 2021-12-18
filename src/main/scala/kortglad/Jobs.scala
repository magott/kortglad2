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
    val work = LazyList.continually {
      db {
        readNextMatchScrapeJob.option
      }
    }
    for
      matchJob <- work.takeWhile(_.isDefined)
      job <- matchJob
    do
      logger.info(s"Matchjob triggered, will add $job")
      val count = RefereeService(db).addSingleMatch(job.matchId)
      logger.info(s"$job update count $count")
      db {
        markMatchScrapeJobCompleted(job.id).run
      }

  def readNextMatchScrapeJob =
    sql"""
        select id, match_id from match_scraper_job order by id limit 1   
    """.query[MatchJob]

  def markMatchScrapeJobCompleted(jobId: Int) =
    sql"""
        delete from match_scraper_job where id = $jobId
     """.update

  case class MatchJob(id: Int, matchId: FiksId) derives Row
}
