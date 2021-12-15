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

  def scrapeMatch(fiksId: FiksId): MatchStat =
    val doc = Jsoup.connect(matchTemplate(fiksId).toString).get()
    parseMatch(fiksId, doc)

  def matchList(fiksId: FiksId) =
    val doc = Jsoup.connect(refereeTemplate(fiksId).toString).get()
    Some(parseMatches(doc))

  def scrapeMatches(fiksId: FiksId): Option[(String, List[FiksId])] =
    val doc = Jsoup.connect(refereeTemplate(fiksId).toString).get()
    val MatchList(refName, idAndKickoffs) = parseMatches(doc)
    Some(
      (
        refName,
        idAndKickoffs.map(_.fiksId)
      )
    )

  def findRefereeStats(fiksId: FiksId) =
    val now = LocalDateTime.now
    System.out.println(s"Preparing to scrape $fiksId")
    scrapeMatches(fiksId).map { case (uri, matches) =>
      val fetched = matches.map(scrapeMatch)
      val result =
        fetched.filter(_.tidspunkt.isBefore(now)) match {
          case h :: t => h :: t.takeWhile(_.inCurrentSeason)
          case x      => x
        }
      RefereeStats.fromMatches(result, uri)
    }

  case class FiksIdAndKickoff(fiksId: FiksId, kickoff: LocalDate)
  case class MatchList(refName: String, idAndKickoffs: List[FiksIdAndKickoff])

  def parseMatches(document: Document): MatchList =
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

    println(fiksId)

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
