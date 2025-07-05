package kortglad

import bloque.db.*
import bloque.http.*, Server.*
import bloque.json.Json
import org.slf4j.LoggerFactory
import java.time.Instant

object App:
  val logger = LoggerFactory.getLogger("Endpoints")
  val health = HealthCheck()
  def run(db: Connections, request: Request): Response =
    request match
      case GET -> path"/referee/${FiksId(fiksId)}" =>
        logger.info(s"GET referee/$fiksId")
        try
          RefereeService(db).updateAndGetRefereeStats(fiksId) match
            case Right(rStats) =>
              StatisticsDatabase(db).addVisit(fiksId, Instant.now())
              logger.info(s"Web search for referee ${rStats.refereeName}")
              Ok(Json(rStats))
            case Left(error) =>
              if error == AppError.GatewayError then health.killMe()
              error.response
        catch
          case e =>
            e.printStackTrace()
            InternalServerError()

      case GET -> path"/search/referee" =>
        val q = request.query.as[Search]
        val refs = RefereeService(db).searchReferee(q)
        Ok(Json(refs))

      case GET -> path"/health" =>
        if (health.isHealthy) Ok() else InternalServerError(health.reason.get())

      case _ => Request.delegate
