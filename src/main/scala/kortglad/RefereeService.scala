package kortglad

import bloque.db.*
import org.slf4j.LoggerFactory

import java.time.OffsetDateTime
import java.time.Year
import java.time.LocalDate

class RefereeService(db: Db) {

  val logger = LoggerFactory.getLogger("RefereeService")

  def updateAndGetRefereeStats(fiksId: FiksId): Option[RefereeStats] = {
    Scraper
      .matchList(fiksId)
      .map { matchList =>
        val toScrape = db {
          val lastSync = upsertReferee(fiksId, matchList.refName)
            .generatedKeys[Option[OffsetDateTime]]("last_sync")
            .unique
          val cutoff = lastSync.map(_.minusDays(3).toLocalDate)

          logger.info(
            s"Last sync for ${matchList.refName} ${lastSync
              .getOrElse("never happened")} will scrape back until ${cutoff.getOrElse("beginning of time")}"
          )

          matchList.idAndKickoffs.filter(idAndKickoff =>
            cutoff.forall(_.isBefore(idAndKickoff.kickoff))
          )
        }

        logger.info(s"Scraping total of ${toScrape.size} matches")

        val matchesPerSeason =
          toScrape
            .map(x => Scraper.scrapeMatch(x.fiksId))
            .groupBy(_.tidspunkt.getYear)

        val seasons = db {
          for case (year, matchStats) <- matchesPerSeason do
            upsertSeason(fiksId, Year.of(year), DbMatchStats(matchStats)).run
          updateLastSync(fiksId).run
          if (!toScrape.isEmpty)
            activateReferee(fiksId).run
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
      matchStats = Scraper.parseMatch(matchId, matchDoc)
    yield db {
      upsertReferee(referee.fiksId, referee.name).run
      upsertMatch(referee.fiksId, matchStats).run
    }
  }
}
