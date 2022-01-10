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
      executor.scheduleWithFixedDelay(
        () => refereeRefresherJob(db),
        0,
        30,
        TimeUnit.DAYS
      )

    def staleDate = OffsetDateTime.now(OSLO).withDayOfYear(1).`with`(java.time.LocalTime.MIN)

    def refereeRefresherJob(db: Db) =
      logger.info("Refresh referee job started")
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
      db{
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
       """.update

  }

  object SingleMatchScraperJob {

    def schedule(db: Db) =
      executor.scheduleWithFixedDelay(
        () => singleMatchScrapeJob(db),
        1,
        120,
        TimeUnit.SECONDS
      )

    def singleMatchScrapeJob(db: Db) =
      logger.info("Match scraper job started")
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
      logger.info("Match scraper job ended")

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

  object TournamentScraperJob{

    def schedule(db: Db) =
      executor.scheduleWithFixedDelay(() => tournamentScraperJob(db), 0, 1, TimeUnit.DAYS)

    def tournamentScraperJob(db: Db) =
      logger.info("Tournament scraper job started")
      val workList = LazyList.continually{
        db {
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
            db{
              addMatchForScraping(matchId).run
              markTournamentScrapeJobCompleted(work).run
            }
          matchIds
      }.recover{ err =>
         logger.info(s"Tournament $work scraping failed, marking as complete ",err)
         db {
           markTournamentScrapeJobCompleted(work).run
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
     """.update

    def addMatchForScraping(matchId: FiksId) =
      sql"""
        insert into match_scraper_job(match_id) values(${matchId.fiksId})
     """.update
  }
}
