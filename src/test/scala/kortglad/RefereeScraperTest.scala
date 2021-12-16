package kortglad

import org.jsoup
import org.jsoup.Jsoup

import java.io.File
import io.Source._

object RefereeScraperTest {

  @main def foo() =
    val file: File =
      File(RefereeScraperTest.getClass.getResource("/match-page.html").getFile)

    val doc = Jsoup.parse(file, "UTF-8")
    val parsed = RefereeScraper.extractRefereeFromSingleMatch(doc)
    println(parsed)

}
