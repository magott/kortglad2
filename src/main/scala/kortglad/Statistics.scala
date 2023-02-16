package kortglad

import bloque.db.Db
import bloque.json.Decoder.number
import bloque.json.{Decoder, Json}

import java.time.{Instant, ZonedDateTime}

object Statistics {
  object LastVisited:
    given Db[LastVisited] = Db
      .tuple(
        Db[FiksId],
        Db[String],
        Db[Long].imap[ZonedDateTime](l => ZonedDateTime.ofInstant(Instant.ofEpochMilli(l),OSLO), _.toInstant.toEpochMilli)
      )
      .as[LastVisited]
  case class LastVisited(fiksId: FiksId, name: String, lastVisit: ZonedDateTime)

}
