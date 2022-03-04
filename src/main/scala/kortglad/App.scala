package kortglad

import bloque.db.*
import bloque.http.*, Server.*
import bloque.json.Json
import org.slf4j.LoggerFactory

object App:
  val logger = LoggerFactory.getLogger("Endpoints")
  def run(db: Sessions): Request ?=> Response =
    request match {
      case GET -> path"/referee/${FiksId(fiksId)}" =>
        logger.info(s"GET referee/$fiksId")
        try {
          RefereeService(db).updateAndGetRefereeStats(fiksId) match
            case Some(rStats) => Ok(Json(rStats))
            case None =>
              NotFound(
                Json(Error(s"Fant ikke dommer med fiks id ${fiksId.fiksId}"))
              )
        } catch {
          case e =>
            e.printStackTrace()
            InternalServerError()
        }
      case GET -> path"/search/referee" =>
        val q = request.query[Search]
        val json = List(
          IndexedReferee(FiksId(2245443), "Morten Andersen-Gott"),
          IndexedReferee(FiksId(3715313), "Marius Wikestad Pedersen")
        )
        val refs = RefereeService(db).searchReferee(q)
        Ok(Json(refs))

      case _ => request.delegate
    }

case class Error(message: String) derives Json
