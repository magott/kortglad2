package kortglad

import org.jsoup
import org.jsoup.Jsoup

import java.io.File
import scala.io.Source.*

class ScraperTest extends munit.FunSuite {

  test("can extract referee and fiks id from match page") {

    val file = ScraperTest.getFile("/new-match-page.html")

    val doc = Jsoup.parse(file, "UTF-8")
    val parsed = Scraper.extractRefereeFromSingleMatch(doc)
    assert(parsed.isDefined)
    parsed.foreach(ref =>
      assertEquals(ref.name, "Morten Andersen-Gott")
      assertEquals(ref.fiksId, FiksId(4081873))
    )
  }

  test("can extract fiks ids from tournament page") {
    val file = ScraperTest.getFile("/tournament-page.html")
    val doc = Jsoup.parse(file, "UTF-8")
    val parsed = Scraper.parseTournament(doc)
    assert(parsed.nonEmpty)
    assertEquals(parsed.size, 132)
  }

  test("can parse match") {
    val file = ScraperTest.getFile("/new-match-page.html")
    val doc = Jsoup.parse(file, "UTF-8")
    val matchStat = Scraper.parseMatch(FiksId(1234), doc).get
    println(matchStat.tournament)
    assert(matchStat.tournament.isDefined)
    assert(matchStat.away == "Åssiden")
    assert(matchStat.cards.yellow == 2)
    assert(matchStat.cards.red == 0)
    assert(matchStat.cards.yellowToRed == 0)
  }

  test("handles hidden referee gracefully") {

    val file = ScraperTest.getFile("/match-page-hidden-ref.html")

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

}

object ScraperTest {
  def getFile(filename: String): File =
    File(ScraperTest.getClass.getResource(filename).getFile)
}
