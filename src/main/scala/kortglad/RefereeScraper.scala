package kortglad

import org.slf4j.{Logger, LoggerFactory}

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import bloque.http.*
import org.jsoup.*
import org.jsoup.nodes.{Document, Element}
import scala.jdk.CollectionConverters.*
import scala.util.Try

object RefereeScraper:
  val log = LoggerFactory.getLogger(getClass)
  val fotballBaseUrl = uri"https://www.fotball.no"

  def refereeTemplate(fiksId: FiksId) =
    fotballBaseUrl / "fotballdata" / "person" / "dommeroppdrag" +? ("fiksId", fiksId.fiksId)

  def matchTemplate(s: FiksId) =
    fotballBaseUrl / "fotballdata" / "kamp" +? ("fiksId", s.fiksId)

  def scrapeSingleMatch(fiksId: FiksId) =
    Try {
      Jsoup.connect(matchTemplate(fiksId).toString).get()
    }.toOption

  def extractRefereeFromSingleMatch(kampDoc: Document) =
    val dommerLink =
      kampDoc.select("div > p > span:contains(Dommer:) + strong > a")
    val fiksId = Uri.fromString(dommerLink.attr("href")).query[FiksId]
    Referee(fiksId, dommerLink.text())

  def scrapeMatch(fiksId: FiksId): MatchStat =
    val doc = Jsoup.connect(matchTemplate(fiksId).toString).get()
    parseMatch(fiksId, doc)

  def matchList(fiksId: FiksId) = Try {
    val doc = Jsoup.connect(refereeTemplate(fiksId).toString).get()
    parseMatchList(doc)
  }.toOption

  def scrapeMatches(fiksId: FiksId): Option[(String, List[FiksId])] =
    val doc = Try {
      Jsoup.connect(refereeTemplate(fiksId).toString).get()
    }.toOption
    doc
      .map(parseMatchList)
      .map(ml => (ml.refName, ml.idAndKickoffs.map(_.fiksId)))

  case class FiksIdAndKickoff(fiksId: FiksId, kickoff: LocalDate)
  case class MatchList(refName: String, idAndKickoffs: List[FiksIdAndKickoff])
  case class Referee(fiksId: FiksId, name: String)
//  case class SingleMatch(refName: String, refFiksId: FiksId, stats: MatchStat)

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
          Uri(java.net.URI(tr.select("td > a").get(1).attr("href")))
            .query[FiksId],
          LocalDate.parse(
            tr.selectFirst("td").text(),
            DateTimeFormatter.ofPattern("dd.MM.yyyy")
          )
        )
      )
      .toList
    log.info(fiksIdAndKickoff.mkString)
    MatchList(refName, fiksIdAndKickoff)

  def parseMatch(fiksId: FiksId, kampDoc: Document) =
    val lag = kampDoc
      .select("span.match__teamname-img")
      .asScala
      .map(_.nextElementSibling().text())
    val home = lag.head
    val away = lag.drop(1).head
    val dateText = kampDoc.select("span.match__arenainfo-date").text()
    val tidspunkt = LocalDateTime.parse(
      dateText,
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH.mm")
    )
    val matchEvents = kampDoc.select("ul.match__events")
    val yellows = matchEvents.select("span.icon-yellow-card--events")
    val yellowReds = matchEvents.select("span.icon-yellow-red-card--events")
    val reds = matchEvents.select("span.icon-red-card--events")

    MatchStat(
      fiksId,
      tidspunkt,
      home,
      away,
      CardStat(yellows.size(), yellowReds.size(), reds.size())
    )

  def hovedDommerIkkeFutsal(rowElement: Element) =
    val cells = rowElement.select("td").asScala
    cells.exists(_.text() == "HD") && cells.forall(
      !_.text().startsWith("Futsal")
    )

  def spiltKamp(rowElement: Element) =
    LocalDate
      .parse(
        rowElement.selectFirst("td").text(),
        DateTimeFormatter.ofPattern("dd.MM.yyyy")
      )
      .isBefore(LocalDate.now())
