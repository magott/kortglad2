package kortglad

import bloque.jetty.*
import bloque.http.*

import Server.*

@main def main = Jetty(8080) {
  request match {
    case GET -> path"/referee/${Var.int(fiksId)}" =>
      RefereeScraper.findRefereeStats(fiksId) match
        case Some(rStats) => Ok(rStats.json)
        case None         => NotFound()

    case _ => NotFound()
  }
}
