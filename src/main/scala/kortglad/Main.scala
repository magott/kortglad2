package kortglad
import bloque.jetty.*
import bloque.http.*
@main def main =
  val server = Jetty(8080)
  import Server.*

  server {
    request match {
      case GET -> path"/referee/${Var.int(fiksId)}" =>
        RefereeScraper.findRefereeStats(fiksId) match
          case Some(rStats) => Ok(rStats.json)
          case None => NotFound()

      case _ => NotFound()
    }
  }