package kortglad

import bloque.db.*
import bloque.http.*, Server.*
import bloque.json.Json
import org.slf4j.LoggerFactory

object App:
  val logger = LoggerFactory.getLogger("Endpoints")
  val health = HealthCheck()
  def run(db: Sessions): Request ?=> Response =
    request match
      case GET -> path"/referee/${FiksId(fiksId)}" =>
        logger.info(s"GET referee/$fiksId")
        try
          RefereeService(db).updateAndGetRefereeStats(fiksId) match
            case Right(rStats) => Ok(Json(rStats))
            case Left(error) =>
              if error == AppError.GatewayError then health.killMe()
              error.response
        catch
          case e =>
            e.printStackTrace()
            InternalServerError()

      case GET -> path"/search/referee" =>
        val q = request.query[Search]
        val refs = RefereeService(db).searchReferee(q)
        Ok(Json(refs))

      case GET -> path"/health" =>
        if (health.isHealthy) Ok() else InternalServerError(health.reason.get())

      case _ => request.delegate
