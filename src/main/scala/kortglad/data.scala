package kortglad

import java.net.URI
import java.text.DecimalFormat
import java.time.{LocalDate, LocalDateTime, Month, Year}
import bloque.http.*
import bloque.db.*

object FiksId:
  given Json[FiksId] = summon[Json[Int]].xmap(apply, _.fiksId)
  given fvar: Var[FiksId] = summon[Var[Int]].xmap(apply, _.fiksId)
  export fvar.unapply

case class FiksId(fiksId: Int) derives Params, Row

given Json[LocalDateTime] =
  summon[Json[String]].xmap(LocalDateTime.parse, _.toString)

given Json[Year] = summon[Json[Int]].xmap(Year.of, _.getValue)
given Row[Year] = Row[Int].imap(Year.of, _.getValue)

case class RefereeSeason(
    season: Year,
    averages: CardAverages,
    totals: CardStat,
    matches: List[MatchStat]
) derives Json

object RefereeStats:
  def fromMatches(matches: List[MatchStat], refereeName: String) =
    val bySeason = matches.groupBy(_.season)
    val seasons = bySeason
      .map((year, matchstats: List[MatchStat]) =>
        val totals = CardStat.totals(matchstats.map(_.cards))
        val averages = CardAverages.from(totals, matchstats.size)
        RefereeSeason(
          Year.of(year),
          averages,
          totals,
          matchstats.sortBy(_.tidspunkt)(Ordering[LocalDateTime].reverse)
        )
      )
      .toList
      .sortBy(_.season)(Ordering[Year].reverse)
    RefereeStats(refereeName, seasons)

case class RefereeStats(
    refereeName: String,
    seasons: List[RefereeSeason]
) derives Json

case class MatchStat(
    fiksId: FiksId,
    tidspunkt: LocalDateTime,
    home: String,
    away: String,
    cards: CardStat
) derives Json:
  def inCurrentSeason = MatchStat.thisSeason(tidspunkt)
  def season = tidspunkt.getYear
  def year = Year.of(season)

object MatchStat:
  def seasonYear =
    if LocalDateTime
        .now()
        .isBefore(LocalDateTime.now().withMonth(Month.MARCH.getValue))
    then Year.now().minusYears(1)
    else Year.now()
  def seasonStart = seasonYear.atMonth(Month.MARCH).atDay(1).atStartOfDay()
  def seasonEnd =
    seasonYear.plusYears(1).atMonth(Month.MARCH).atDay(1).atStartOfDay()
  def thisSeason(d: LocalDateTime) =
    d.isBefore(seasonEnd) && d.isAfter(seasonStart)

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
