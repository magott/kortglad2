package kortglad

import bloque.db.*
import bloque.http.*, Server.*

object App:
  def run(db: Db): Request ?=> Response =
    request match {
      case GET -> path"/referee/${FiksId(fiksId)}" =>
        try {
          RefereeService(db).updateAndGetRefereeStats(fiksId) match
            case Some(rStats) => Ok(rStats.json)
            case None =>
              NotFound(
                Error(s"Fant ikke dommer med fiks id ${fiksId.fiksId}").json
              )
        } catch {
          case e =>
            e.printStackTrace()
            InternalServerError()
        }

      case _ => request.delegate
    }

case class Error(message: String) derives Json
