package kortglad

import bloque.db.*
import org.slf4j.LoggerFactory

import java.time.OffsetDateTime
import java.time.Year

class RefereeService(db: Db) {

  val logger = LoggerFactory.getLogger("RefereeService")

  def updateAndGetRefereeStats(fiksId: FiksId) = {
    RefereeScraper
      .matchList(fiksId)
      .map { matchList =>
        val toScrape = db {
          val lastSync = upsertReferee(fiksId, matchList.refName)
            .generatedKeys[Option[OffsetDateTime]]("last_sync")
            .unique
          val cutoff = lastSync.map(_.minusDays(3).toLocalDate)
          matchList.idAndKickoffs.filter(idAndKickoff =>
            cutoff.forall(_.isAfter(idAndKickoff.kickoff))
          )
        }

        logger.info(s"Scraping ${toScrape.size}")

        val matchesPerSeason =
          toScrape
            .map(x => RefereeScraper.scrapeMatch(x.fiksId))
            .groupBy(_.tidspunkt.getYear)

        val seasons = db {
          for case (year, matchStats) <- matchesPerSeason do
            upsertSeason(fiksId, Year.of(year), DbMatchStats(matchStats)).run
          updateLastSync(fiksId).run
          refereeSeasonsByRefereeId(fiksId).to(List)
        }
        RefereeStats.fromMatches(
          seasons.flatMap(_.matchStats.matchStats),
          matchList.refName
        )
      }
  }

}
