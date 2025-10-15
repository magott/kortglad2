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

  //Ok v3
  def extractRefereeFromSingleMatch(kampDoc: Document) =
    val dommerElement = kampDoc.select("div[data-tab=dommere]")
    //Bruker regex for å få med kun elementet for Dommer, ikke Assistentdommer 4. dommer osv.
    val dommerLink = dommerElement.select("""tr:has(td:matches(^Dommer$)) a""")
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

  //Ok for v3
  def parseTournament(doc: Document) = {
    doc
      .select("div#PrevMatchesContainer a[href*=/kamp/]")
      .asScala
      .map(row =>
        Uri
          .fromString(row.attr("href"))
          .query
          .as[FiksId]
      )
      .toSet
  }

  case class FiksIdAndKickoff(fiksId: FiksId, kickoff: LocalDate)
  case class MatchList(refName: String, idAndKickoffs: List[FiksIdAndKickoff])
  case class Referee(fiksId: FiksId, name: String) derives Db

  def parseMatchList(document: Document): MatchList =
    val body = document.body()
    val refName = body.select("h1.personName").text()
    log.info(s"Scraping referee named $refName")
    val oppdragsrader = body
      .select("tbody > tr")
      .asScala
    val dommerRader = oppdragsrader
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
    logger.info(s"Scraping match with fiks id $matchId")
    //For å unngå å hente resultater fra tidliger oppgjør, isoler elementet som har kampfakta øverst på siden
    val kampfaktaElement = kampDoc.select("div[data-page-name=MatchPage_Index]")
    val result =
      kampfaktaElement
        .select(
          " .a_atomicGrid ul > li.atomicHalfWidth > div.a_matchCard > div.cardContent > div.result > div.endResult"
        )
        .text()
    val resultIsSet = !result.isBlank
    if (resultIsSet) {
      val lag = kampfaktaElement
        .select("div.cardContent > div.teamName")
        .asScala
        .map(_.text())
      val tournament =
        // Finner 'sibling' a element til span som inneholder 'Turnering:'
        kampfaktaElement.select("p > span:contains(Turnering:) + a").text()
      val home = lag.head
      val away = lag.drop(1).head
      val datoRegex = """^\s*\S+\s+(\d{2}\.\d{2}\.\d{2})$"""
      val klokkeslettRegex = """^\d{2}\:\d{2}$"""
      val datoText = kampfaktaElement
        .select(
          s"div.matchHeading > div.headingElements > span.headingElement:matches($datoRegex)"
        )
        .text()
        .split("""\s+""")
        .toList
        .last
      val klokkeslettText = kampfaktaElement
        .select(
          s"div.matchHeading > div.headingElements > span.headingElement:matches($klokkeslettRegex)"
        )
        .text()

      val tidspunkt = LocalDateTime.parse(
        s"$datoText $klokkeslettText",
        DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")
      )
      val matchEvents = kampDoc.select("div[data-tab=kamphendelser]")
      val yellows =
        matchEvents.select(
          "div.timelineEventLine > div.timelineEvent > div[data-icon=YellowCard2]"
        )
      val yellowReds =
        matchEvents.select(
          "div.timelineEventLine > div.timelineEvent > div[data-icon=RedYellowRedCard2]"
        )
      val reds = matchEvents.select(
        "div.timelineEventLine > div.timelineEvent > div[data-icon=RedCard2]"
      )

      val stat = MatchStat(
        matchId,
        tidspunkt,
        Option.unless(tournament.isBlank) {
          tournament
        },
        home,
        away,
        CardStat(yellows.size(), yellowReds.size(), reds.size())
      )
      log.info("Successfully parsed match: " + stat)
      Some(
        stat
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
          DateTimeFormatter.ofPattern("HH:mm")
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
