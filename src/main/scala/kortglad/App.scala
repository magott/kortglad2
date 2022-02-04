package kortglad

import bloque.db.*
import bloque.http.*, Server.*
import org.slf4j.LoggerFactory

object App:
  val logger = LoggerFactory.getLogger("Endpoints")
  def run(db: Db): Request ?=> Response =
    request match {
      case GET -> path"/referee/${FiksId(fiksId)}" =>
        logger.info(s"GET referee/$fiksId")
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
      case GET -> path"/search/referee" =>
        val q = request.query[Search]
        val json = List(
          IndexedReferee(FiksId(2245443), "Morten Andersen-Gott"),
          IndexedReferee(FiksId(3715313), "Marius Wikestad Pedersen")
        )
        val refs = RefereeService(db).searchReferee(q)
        Ok(refs.json)

      case _ => request.delegate
    }

case class Error(message: String) derives Json
