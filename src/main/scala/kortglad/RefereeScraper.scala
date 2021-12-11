package kortglad

import org.slf4j.{Logger, LoggerFactory}

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

  def refereeTemplate(fiksId: Int) =
    fotballBaseUrl / "fotballdata" / "person" / "dommeroppdrag" +? ("fiksId", fiksId)

  def scrapeMatch(matchUri: Uri): MatchStat =
    val doc = Jsoup.connect(matchUri.toString).get()
    parseMatch(matchUri, doc)

  def scrapeMatches(fiksId: Int) = Try {
    val doc = Jsoup.connect(refereeTemplate(fiksId).toString).get()
    def fix(s: String) = fotballBaseUrl.updated(path = Path(s))
    val (refName, urls) = parseMatches(fiksId, doc)
    (refName, urls.map(fix))
  }.toOption

  def findRefereeStats(fiksId: Int) =
    val now = LocalDateTime.now
    System.out.println(s"Prepareing to scrape $fiksId")
    scrapeMatches(fiksId).map { case (uri, matches) =>
      val fetched = matches.map(scrapeMatch)
      val result =
        fetched.filter(_.tidspunkt.isBefore(now)) match {
          case h :: t => h :: t.takeWhile(_.inCurrentSeason)
          case x      => x
        }
      RefereeStats(result, uri)
    }

  def parseMatches(fiksId: Int, document: Document): (String, List[String]) =
    val body = document.body()
    val refName = body.select(".fiks-header--person").select("h1 > a").text()
    val dommerRader = body
      .select("tr")
      .asScala
      .filter(hovedDommerIkkeFutsal)
    val urls = dommerRader
      .flatMap(_.select("td > a").asScala.map(_.attr("href")))
      .filter(_.contains("/kamp/"))
      .toList
    log.info(s"Scraping referee named $refName")
    (refName, urls)

  def parseMatch(matchUri: Uri, kampDoc: Document) =
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

    println(matchUri)

    MatchStat(
      matchUri.query[FiksId],
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
