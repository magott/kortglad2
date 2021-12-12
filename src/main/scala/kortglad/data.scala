package kortglad

import java.net.URI
import java.text.DecimalFormat
import java.time.{LocalDate, LocalDateTime, Month, Year}
import bloque.http.*

object FiksId:
  given Json[FiksId] = summon[Json[Int]].xmap(apply, _.fiksId)

case class FiksId(fiksId: Int) derives Params

given Json[LocalDateTime] =
  summon[Json[String]].xmap(LocalDateTime.parse, _.toString)

object RefereeStats:
  def fromMatches(matches: List[MatchStat], refereeName: String) =
    val totals = CardStat.totals(matches.map(_.cards))
    val averages = CardAvarages.from(totals, matches.size)
    RefereeStats(matches, refereeName, totals, averages)

case class RefereeStats(
    matches: List[MatchStat],
    refereeName: String,
    totals: CardStat,
    averages: CardAvarages
) derives Json

case class MatchStat(
    fiksId: FiksId,
    tidspunkt: LocalDateTime,
    home: String,
    away: String,
    cards: CardStat
) derives Json:
  def inCurrentSeason = MatchStat.thisSeason(tidspunkt)

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

case class CardAvarages(yellow: Double, yellowToRed: Double, red: Double)
    derives Json:
  val formatter = new DecimalFormat("0.00")
  def snittPretty =
    s"Snitt: Gule ${formatter.format(yellow)}, Gult nr 2: ${formatter
      .format(yellowToRed)}, Røde ${formatter.format(red)}"

object CardAvarages:
  def from(totals: CardStat, matches: Int) =
    import totals.*
    CardAvarages(
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
