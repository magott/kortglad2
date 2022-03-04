package kortglad

import org.jsoup
import org.jsoup.Jsoup

import java.io.File
import io.Source._

class ScraperTest extends munit.FunSuite {

  test("can extract referee and fiks id from match page") {

    val file = ScraperTest.getFile("/match-page.html")

    val doc = Jsoup.parse(file, "UTF-8")
    val parsed = Scraper.extractRefereeFromSingleMatch(doc)
    assert(parsed.isDefined)
    parsed.foreach(ref =>
      assertEquals(ref.name, "Morten Andersen-Gott")
      assertEquals(ref.fiksId, FiksId(2245443))
    )
  }

  test("can extract fiks ids from tournament page") {
    val file = ScraperTest.getFile("/tournament-page.html")
    val doc = Jsoup.parse(file, "UTF-8")
    val parsed = Scraper.parseTournament(doc)
    assert(parsed.nonEmpty)
    assertEquals(parsed.size, 20)
  }

  test("can parse match") {
    val file = ScraperTest.getFile("/match-page.html")
    val doc = Jsoup.parse(file, "UTF-8")
    val matchStat = Scraper.parseMatch(FiksId(1234), doc)
    println(matchStat.tournament)
    assert(matchStat.tournament.isDefined)
  }

  test("handles hidden referee gracefully") {

    val file = ScraperTest.getFile("/match-page-hidden-ref.html")

    val doc = Jsoup.parse(file, "UTF-8")
    val parsed = Scraper.extractRefereeFromSingleMatch(doc)
    assert(parsed.isEmpty, "Hidden referee should return None")
  }

}

object ScraperTest {
  def getFile(filename: String): File =
    File(ScraperTest.getClass.getResource(filename).getFile)
}
