package kortglad

import bloque.http.*, Server.*

object App:
  def run: Request ?=> Response =
    request match {
      case GET -> path"/referee/${Var.int(fiksId)}" =>
        RefereeScraper.findRefereeStats(fiksId) match
          case Some(rStats) => Ok(rStats.json)
          case None         => NotFound()

      case _ => Response.delegate
    }
