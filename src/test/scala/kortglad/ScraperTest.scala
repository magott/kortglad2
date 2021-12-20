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
    assertEquals(parsed.name, "Morten Andersen-Gott")
    assertEquals(parsed.fiksId, FiksId(2245443))
  }

  test("can extract fiks ids from tournament page") {
    val file = ScraperTest.getFile("/tournament-page.html")
    val doc = Jsoup.parse(file, "UTF-8")
    val parsed = Scraper.parseTournament(doc)
    assert(parsed.nonEmpty)
    assertEquals(parsed.size, 20)
  }

}

object ScraperTest {
  def getFile(filename: String): File =
    File(ScraperTest.getClass.getResource(filename).getFile)
}
