package kortglad
import bloque.db.*
import org.slf4j.LoggerFactory

import java.time.{Instant, LocalDate, LocalDateTime, Month, OffsetDateTime, Year, ZoneId, ZoneOffset, ZonedDateTime}
import java.util.concurrent.{Executors, TimeUnit}
import scala.util.Try

object Jobs {

  lazy val executor = Executors.newSingleThreadScheduledExecutor()
  val logger = LoggerFactory.getLogger("Jobs")

  object RefereeRefresherJob {
    val OSLO = ZoneId.of("Europe/Oslo")

    def schedule(db: Db) =
      executor.scheduleAtFixedRate(
        () => refereeRefresherJob(db),
        0,
        30,
        TimeUnit.DAYS
      )

    def refereeRefresherJob(db: Db) =
      val staleDate = LocalDate
        .now()
        .atStartOfDay()
        .withDayOfYear(1)
        .atOffset(OSLO.getRules.getOffset(Instant.now()))
      logger.info("Refresh job started")
      val workList = LazyList.continually {
        db {
          findStaleReferees(staleDate).option
        }
      }
      for
        work <- workList.takeWhile(_.isDefined)
        fiksId <- work
      do
        logger.info(s"Refreshing stale referee data for $fiksId")
        val updated = Try{
          RefereeService(db).updateAndGetRefereeStats(fiksId)
        }.recover {
          case e:_ => logger.error(s"Referee refresh failed for $fiksId", e)
            db{
              updateLastSync(fiksId).run //TODO New column for failed sync?
            }
            None
        }.toOption.flatten
        logger.info(
          s"Referee $fiksId has ${updated.map(_.totalNumberOfMatches).getOrElse("failed")} indexed"
        )

    def findStaleReferees(staleDate: OffsetDateTime) =
      sql"""
       select fiks_id from referee where last_sync is null or referee.last_sync < $staleDate order by last_sync limit 1    
       """.query[FiksId]
  }

  object SingleMatchScraperJob {

    def schedule(db: Db) =
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
        logger.info(s"Matchjob found match, will add $job")
        val count = Try {
          RefereeService(db).addSingleMatch(job.matchId)
        }.recover{
          case e:_  => logger.error(s"Match scraper job failed for ${job.matchId}, will be marked as completed", e)
            0
        }
        logger.info(s"Match scraper $job update count ${count.getOrElse("nothing happened")}")
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
}
