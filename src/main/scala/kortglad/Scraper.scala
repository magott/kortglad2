package kortglad

import org.slf4j.{Logger, LoggerFactory}

import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import bloque.http.*
import bloque.db.Db
import org.jsoup.*
import org.jsoup.nodes.{Document, Element}
import scala.jdk.CollectionConverters.*
import scala.util.Try
import scala.concurrent.duration._

object Scraper:
  val log = LoggerFactory.getLogger(getClass)
  val fotballdata = uri"https://www.fotball.no" / "fotballdata"

  def refereeTemplate(fiksId: FiksId) =
    fotballdata / "person" / "dommeroppdrag" +? fiksId

  def matchTemplate(fiksId: FiksId) =
    fotballdata / "kamp" +? fiksId

  def tournamentTemplate(fiksId: FiksId) =
    fotballdata / "turnering" / "terminliste" +? fiksId

  def scrapeSingleMatch(fiksId: FiksId) =
    Try {
      Jsoup.connect(matchTemplate(fiksId).toString).get()
    }.toOption

  def extractRefereeFromSingleMatch(kampDoc: Document) =
    val dommerLink =
      kampDoc.select("div > p > span:contains(Dommer:) + strong > a")
    if (dommerLink.isEmpty) None
    else
      val fiksId = Uri.fromString(dommerLink.attr("href")).query.as[FiksId]
      Some(Referee(fiksId, dommerLink.text()))

  def scrapeMatch(matchId: FiksId): Option[MatchStat] =
    val doc = Jsoup.connect(matchTemplate(matchId).toString).get()
    parseMatch(matchId, doc)

  def matchList(fiksId: FiksId) = Try {
    log.info(s"Getting matchlist for $fiksId")
    val doc =
      Jsoup
        .connect(refereeTemplate(fiksId).toString)
        .timeout(25.seconds.toMillis.toInt)
        .get()
    log.info(s"Got matchlist for $fiksId, start parsing")
    parseMatchList(doc)
  }.toEither.left.map {
    case r: HttpStatusException if r.getStatusCode == 404 =>
      AppError.RefereeNotFound(fiksId)
    case _ =>
      AppError.GatewayError
  }

  def scrapeMatches(fiksId: FiksId): Option[(String, List[FiksId])] =
    val doc = Try {
      Jsoup.connect(refereeTemplate(fiksId).toString).get()
    }.toOption
    doc
      .map(parseMatchList)
      .map(ml => (ml.refName, ml.idAndKickoffs.map(_.fiksId)))

  def readTournament(fiksId: FiksId) =
    Jsoup.connect(tournamentTemplate(fiksId).toString).get()

  def parseTournament(doc: Document) =
    doc
      .select("tbody > tr:not(.upcoming-match)")
      .asScala
      .filter { row =>
        dateElementToLocalDate(row.selectFirst("td.table--mobile__date"))
          .isBefore(LocalDate.now().plusDays(1)) &&
        row.select("td.table--mobile__result > a").text().exists(_.isDigit)
      }
      .map(row =>
        Uri
          .fromString(row.select("td.table--mobile__result > a").attr("href"))
          .query
          .as[FiksId]
      )
      .toSet

  case class FiksIdAndKickoff(fiksId: FiksId, kickoff: LocalDate)
  case class MatchList(refName: String, idAndKickoffs: List[FiksIdAndKickoff])
  case class Referee(fiksId: FiksId, name: String) derives Db

  def parseMatchList(document: Document): MatchList =
    val body = document.body()
    val refName = body.select(".fiks-header--person").select("h1 > a").text()
    log.info(s"Scraping referee named $refName")
    val dommerRader = body
      .select("tr")
      .asScala
      .filter(row => hovedDommerIkkeFutsal(row) && spiltKamp(row))
    val fiksIdAndKickoff = dommerRader
      .map(tr =>
        FiksIdAndKickoff(
          Uri
            .fromString(tr.select("td > a").get(1).attr("href"))
            .query
            .as[FiksId],
          dateElementToLocalDate(
            tr.selectFirst("td")
          )
        )
      )
      .toList
    log.info(
      s"Matchlist for $refName ${fiksIdAndKickoff.map(f => f.fiksId.fiksId -> f.kickoff).mkString}"
    )
    MatchList(refName, fiksIdAndKickoff)

  def parseMatch(matchId: FiksId, kampDoc: Document) =
    val result =
      kampDoc
        .select("div.resultWrapper > div.result")
        .text()
    val resultIsSet = !result.isBlank
    if (resultIsSet) {
      val lag = kampDoc
        .select("div.badgeCardTitle")
        .asScala
        .map(_.text())
      val tournament =
        kampDoc.select("div > p:contains(Turnering:) > a").text()
      val home = lag.head
      val away = lag.drop(1).head
      val datoRegex =
        """\b(0[1-9]|[12][0-9]|3[01])\.(0[1-9]|1[0-2])\.\d{4}\s(0[0-9]|1[0-9]|2[0-3])\.[0-5][0-9]\b"""
      val dateText = kampDoc.select(s"strong > span:matches($datoRegex)").text()
      val tidspunkt = LocalDateTime.parse(
        dateText,
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH.mm")
      )
      val matchEvents = kampDoc.select("div:contains(Kamphendelser)")
      val yellows =
        kampDoc.select("div.timelineEvent > div[data-icon=YellowCard]")
      val yellowReds =
        kampDoc.select("div.timelineEvent > div[data-icon=RedYellowRedCard]")
      val reds = kampDoc.select("div.timelineEvent > div[data-icon=RedCard]")

      Some(
        MatchStat(
          matchId,
          tidspunkt,
          Option.unless(tournament.isBlank) {
            tournament
          },
          home,
          away,
          CardStat(yellows.size(), yellowReds.size(), reds.size())
        )
      )
    } else {
      log.info(s"$matchId does not have a final score reported, skipping")
      None
    }

  def hovedDommerIkkeFutsal(rowElement: Element) =
    val cells = rowElement.select("td").asScala
    cells.exists(_.text() == "HD") && cells.forall(
      !_.text().contains("Futsal")
    )

  def spiltKamp(rowElement: Element) =
    LocalDate
      .parse(
        rowElement.selectFirst("td").text().trim,
        DateTimeFormatter.ofPattern("dd.MM.yyyy")
      )
      .atTime(
        LocalTime.parse(
          rowElement.select("td").get(1).text().trim,
          DateTimeFormatter.ofPattern("HH.mm")
        )
      )
      .isBefore(
        LocalDateTime.now().plusHours(2)
      ) //Spilt kamp om startidspunkt er 2 timer etter nå

  def dateElementToLocalDate(element: Element) =
    LocalDate.parse(
      element.text(),
      DateTimeFormatter.ofPattern("dd.MM.yyyy")
    )
