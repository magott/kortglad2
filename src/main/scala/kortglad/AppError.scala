package kortglad

import bloque.http.*
import bloque.json.*

enum AppError derives Json:
  case RefereeNotFound(fiksId: FiksId)
  case GatewayError

  def response(using Request) = this match
    case RefereeNotFound(_) => Status.NotFound(Json(this))
    case GatewayError       => Status.GatewayTimeout(Json(this))
