package kortglad

import java.net.URI
import java.text.DecimalFormat
import java.time.{LocalDate, LocalDateTime, Month, Year}
import bloque.http.*
import bloque.db.*
import bloque.json.Json

object FiksId:
  given Json[FiksId] = summon[Json[Int]].imap(apply, _.fiksId)
  given fvar: Val[FiksId] = summon[Val[Int]].xmap(apply, _.fiksId, "fiksId")
  export fvar.unapply

case class FiksId(fiksId: Int) derives Params, Db

case class Search(q: String) derives Params

case class IndexedReferee(fiksId: FiksId, name: String) derives Json, Db

given Json[LocalDateTime] =
  summon[Json[String]].imap(LocalDateTime.parse, _.toString)

given Json[Year] = summon[Json[Int]].imap(Year.of, _.getValue)
given Db[Year] = Db[Int].imap(Year.of, _.getValue)

case class RefereeSeason(
    year: Year,
    averages: CardAverages,
    totals: CardStat,
    matches: List[MatchStat]
) derives Json

object RefereeStats:
  def fromMatches(matches: List[MatchStat], refereeName: String) =
    val bySeason = matches.groupBy(_.year)
    val seasons = bySeason
      .map((year, matchstats: List[MatchStat]) =>
        val totals = CardStat.totals(matchstats.map(_.cards))
        val averages = CardAverages.from(totals, matchstats.size)
        RefereeSeason(
          year,
          averages,
          totals,
          matchstats.sortBy(_.tidspunkt)(Ordering[LocalDateTime].reverse)
        )
      )
      .toList
      .sortBy(_.year)(Ordering[Year].reverse)
    RefereeStats(refereeName, seasons)

case class RefereeStats(
    refereeName: String,
    seasons: List[RefereeSeason]
) derives Json:
  def totalNumberOfMatches =
    seasons.map(_.matches.size).sum

case class MatchStat(
    fiksId: FiksId,
    tidspunkt: LocalDateTime,
    tournament: Option[String],
    home: String,
    away: String,
    cards: CardStat
) derives Json:
  def year = Year.of(tidspunkt.getYear)

case class CardAverages(yellow: Double, yellowToRed: Double, red: Double)
    derives Json:
  val formatter = new DecimalFormat("0.00")
  def snittPretty =
    s"Snitt: Gule ${formatter.format(yellow)}, Gult nr 2: ${formatter
      .format(yellowToRed)}, Røde ${formatter.format(red)}"

object CardAverages:
  def from(totals: CardStat, matches: Int) =
    import totals.*
    CardAverages(
      yellow.toDouble / matches,
      yellowToRed.toDouble / matches,
      red.toDouble / matches
    )

case class CardStat(yellow: Int, yellowToRed: Int, red: Int) derives Json:
  def pretty = s"Gule $yellow, Gult nr 2: $yellowToRed, Røde $red"

  def +(cardStat: CardStat) =
    CardStat(
      yellow + cardStat.yellow,
      yellowToRed + cardStat.yellowToRed,
      red + cardStat.red
    )

object CardStat:
  def empty = new CardStat(0, 0, 0)
  def totals(cardStats: List[CardStat]) = cardStats.foldLeft(empty)(_ + _)
