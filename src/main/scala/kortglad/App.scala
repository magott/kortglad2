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
            case None         => NotFound()
        } catch {
          case e =>
            e.printStackTrace()
            InternalServerError()
        }

      case _ => request.delegate
    }
