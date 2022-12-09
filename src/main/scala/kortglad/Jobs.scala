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

    def schedule(db: Connections) =
      executor.scheduleWithFixedDelay(
        () => refereeRefresherJob(db),
        0,
        30,
        TimeUnit.DAYS
      )

    def staleDate = OffsetDateTime.now(OSLO).withDayOfYear(1).`with`(java.time.LocalTime.MIN)

    def refereeRefresherJob(db: Connections) =
      logger.info(s"Refresh referee job started refershing referees not synced since before ${staleDate}")
      val workList = LazyList.continually {
        db.tx {
          findStaleReferees(staleDate).option
        }
      }
      for
        work <- workList.takeWhile(_.isDefined)
        fiksId <- work
      do
        logger.info(s"Refreshing stale referee data for $fiksId")
        RefereeService(db).updateAndGetRefereeStats(fiksId) match
          case Left(err) =>
            logger.error(s"Referee refresh failed for $fiksId", err)
          case Right(updated) =>
            logger.info(
              s"Referee $fiksId has ${updated.totalNumberOfMatches} indexed"
            )

        db.tx{
          updateLastSync(fiksId).update //TODO New column for failed sync?
        }

      db.tx{
        logger.info("Looking for inactive referees to deactivate from future refresh")
        val deactivated = inactivateRefereesWithNoMatchesSinceYearBefore(Year.now()).generatedKeys[(Int, String)]("fiks_id", "name").to(List)
        logger.info(s"Found ${deactivated.size} referees that were deactivated ${deactivated.mkString("[", ",", "]")}")
      }
      logger.info("Refresh referee job ended")

    def findStaleReferees(staleDate: OffsetDateTime) =
      sql"""
       select fiks_id from referee where active is true and (last_sync is null or referee.last_sync < $staleDate) order by last_sync limit 1
       """.query[FiksId]

    def inactivateRefereesWithNoMatchesSinceYearBefore(year: Year) =
      sql"""
    update referee set active = false
    where active = true
    and referee.fiks_id in
          (select r.fiks_id from referee r
            left outer join referee_season rs on r.fiks_id = rs.referee_id
            where (last_sync is not null and extract(YEAR from last_sync) >= ${year.getValue})
            group by r.fiks_id, rs.referee_id
            having max(year) is null or max(year) < ${year.minusYears(1).getValue})
       """

  }

  object SingleMatchScraperJob {

    def schedule(db: Connections) =
      executor.scheduleWithFixedDelay(
        () => singleMatchScrapeJob(db),
        1,
        60,
        TimeUnit.MINUTES
      )

    def singleMatchScrapeJob(db: Connections) =
      logger.info("Match scraper job started")
      val work = LazyList.continually {
        db.tx {
          readNextMatchScrapeJob.option
        }
      }
      for
        matchJob <- work.takeWhile(_.isDefined)
        job <- matchJob
      do
        logger.info(s"Matchjob found match, will add $job")
        val count = try
          RefereeService(db).addSingleMatch(job.matchId)
        catch
          case e  => logger.error(s"Match scraper job failed for ${job.matchId}, will be marked as completed", e)
            None

        logger.info(s"Match scraper $job update count ${count.getOrElse("nothing happened")}")
        db.tx {
          markMatchScrapeJobCompleted(job.id).update
        }
      logger.info("Match scraper job ended")

    def readNextMatchScrapeJob =
      sql"""
        select id, match_id from match_scraper_job order by id limit 1   
    """.query[MatchJob]

    def markMatchScrapeJobCompleted(jobId: Int) =
      sql"""
        delete from match_scraper_job where id = $jobId
     """

    case class MatchJob(id: Int, matchId: FiksId) derives Db
  }

  object TournamentScraperJob{

    def schedule(db: Connections) =
      executor.scheduleWithFixedDelay(() => tournamentScraperJob(db), 0, 1, TimeUnit.DAYS)

    def tournamentScraperJob(db: Connections) =
      logger.info("Tournament scraper job started")
      val workList = LazyList.continually{
        db.tx {
          readNextTournamentScraperJob.option
        }
      }
      for
        tournamentJob <- workList.takeWhile(_.isDefined)
        work <- tournamentJob
      do
       Try {
          val doc = Scraper.readTournament(work)
          val matchIds = Scraper.parseTournament(doc)
          for
            matchId <- matchIds
          do
            db.tx{
              addMatchForScraping(matchId).update
              markTournamentScrapeJobCompleted(work).update
            }
          matchIds
      }.recover{ err =>
         logger.info(s"Tournament $work scraping failed, marking as complete ",err)
         db.tx {
           markTournamentScrapeJobCompleted(work).update
         }
         List.empty
     }
      .foreach{ ids =>
         logger.info(s"Tournament $work scraped ${ids.size} matches found" )
      }
      logger.info("Tournament job ended")




    def readNextTournamentScraperJob =
      sql"""
        select tournament_id from tournament_scraper_job order by id limit 1
       """.query[FiksId]

    def markTournamentScrapeJobCompleted(fiksId: FiksId) =
      sql"""
        delete from tournament_scraper_job where tournament_id = ${fiksId.fiksId}
     """

    def addMatchForScraping(matchId: FiksId) =
      sql"""
        insert into match_scraper_job(match_id) values(${matchId.fiksId})
     """
  }
}
