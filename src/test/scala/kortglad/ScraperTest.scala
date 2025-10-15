package kortglad

import org.jsoup
import org.jsoup.Jsoup

import java.io.File
import scala.io.Source.*

class ScraperTest extends munit.FunSuite {

  test("can extract referee and fiks id from match page") {

    val file = ScraperTest.getFile("/matchpage-v3.html")

    val doc = Jsoup.parse(file, "UTF-8")
    val parsed = Scraper.extractRefereeFromSingleMatch(doc)
    assert(parsed.isDefined)
    parsed.foreach(ref =>
      assertEquals(ref.name, "Mohammad Usman Aslam")
      assertEquals(ref.fiksId, FiksId(2235467))
    )
  }

  test("can extract fiks ids from tournament page") {
    val file = ScraperTest.getFile("/tournament-page-v3.html")
    val doc = Jsoup.parse(file, "UTF-8")
    val parsed = Scraper.parseTournament(doc)
    assert(parsed.nonEmpty)
    assertEquals(parsed.size, 77)
  }

  test("can parse match") {
    val file = ScraperTest.getFile("/matchpage-v3.html")
    val doc = Jsoup.parse(file, "UTF-8")
    val matchStat = Scraper.parseMatch(FiksId(1234), doc).get
    println(matchStat.tournament)
    assert(matchStat.tournament.isDefined)
    assert(matchStat.away == "Molde")
    assert(matchStat.home == "Brann")
    assert(matchStat.cards.yellow == 11)
    assert(matchStat.cards.red == 1)
    assert(matchStat.cards.yellowToRed == 1)
  }

  test("can parse match with team name without link") {
    val file = ScraperTest.getFile("/matchpage-v3-team-without-links.html")
    val doc = Jsoup.parse(file, "UTF-8")
    val matchStat = Scraper.parseMatch(FiksId(1234), doc).get
    println(matchStat.tournament)
    assert(matchStat.tournament.isDefined)
    assert(matchStat.home == "Nordstrand")
    assert(matchStat.away == "Gamle Oslo")

  }

  test("handles hidden referee gracefully") {

    val file = ScraperTest.getFile("/matchpage-v3-hidden-ref.html")

    val doc = Jsoup.parse(file, "UTF-8")
    val parsed = Scraper.extractRefereeFromSingleMatch(doc)
    assert(parsed.isEmpty, "Hidden referee should return None")
  }

  test("will not scrape postponed match") {

    val file = ScraperTest.getFile("/match-page-postponed-game.html")

    val doc = Jsoup.parse(file, "UTF-8")
    val parsed = Scraper.parseMatch(FiksId(1234), doc)
    assert(parsed.isEmpty, "Postponed match should return None")
  }

  test("can scrape matchlist for referee and name") {
    val file = ScraperTest.getFile("/match-list-2025-10.html")
    val doc = Jsoup.parse(file, "UTF-8")
    val parsed = Scraper.parseMatchList(doc)
    assert(parsed.refName == "Morten Andersen-Gott")
  }

}

object ScraperTest {
  def getFile(filename: String): File =
    File(ScraperTest.getClass.getResource(filename).getFile)
}
