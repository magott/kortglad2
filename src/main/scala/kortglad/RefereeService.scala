package kortglad

import bloque.db.*
import org.slf4j.LoggerFactory

import java.time.OffsetDateTime
import java.time.Year
import java.time.LocalDate

class RefereeService(db: Connections) {

  val logger = LoggerFactory.getLogger("RefereeService")

  def updateAndGetRefereeStats(
      fiksId: FiksId
  ): Either[AppError, RefereeStats] = {
    Scraper
      .matchList(fiksId)
      .map { matchList =>
        val toScrape = db.tx {
          val lastSync = upsertReferee(fiksId, matchList.refName)
            .generatedKeys[Option[OffsetDateTime]]("last_sync")
            .unique
          val syncMatchesAfter = lastSync.map(_.minusDays(3).toLocalDate)

          logger.info(
            s"Last sync for ${matchList.refName} ${lastSync
              .getOrElse("never happened")} will scrape back until ${syncMatchesAfter.getOrElse("beginning of time")}"
          )

          matchList.idAndKickoffs.filter(idAndKickoff =>
            syncMatchesAfter.forall(_.isBefore(idAndKickoff.kickoff))
          )
        }

        logger.info(s"Scraping total of ${toScrape.size} matches")

        val matchesPerSeason =
          toScrape
            .flatMap(x => Scraper.scrapeMatch(x.fiksId))
            .groupBy(_.tidspunkt.getYear)

        val seasons = db.tx {
          for case (year, matchStats) <- matchesPerSeason do
            upsertSeason(fiksId, Year.of(year), DbMatchStats(matchStats)).update
          updateLastSync(fiksId).update
          if (toScrape.nonEmpty)
            activateReferee(fiksId).update
          refereeSeasonsByRefereeId(fiksId).to(List)
        }
        RefereeStats.fromMatches(
          seasons.flatMap(_.matchStats.matchStats),
          matchList.refName
        )
      }
  }

  def addSingleMatch(matchId: FiksId) = {
    for
      matchDoc <- Scraper.scrapeSingleMatch(matchId)
      referee <- Scraper.extractRefereeFromSingleMatch(matchDoc)
      matchStats <- Scraper.parseMatch(matchId, matchDoc)
    yield db.tx {
      upsertReferee(referee.fiksId, referee.name).update
      upsertMatch(referee.fiksId, matchStats).update
    }
  }

  def searchReferee(search: Search) = {
    db.tx {
      searchReferees(search.q).to(List)
    }
  }
}
