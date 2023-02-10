package kortglad

import bloque.db.{Connections, sql}

import java.time.Instant
class StatisticsDatabase(val db: Connections) {

  def addVisit(refereeId: FiksId, timestamp: Instant) = db.tx {
    addVisitSql(refereeId, timestamp).update
  }

  private def addVisitSql(refereeId: FiksId, timestamp: Instant) =
    sql"""
       insert into referee_visit(referee_id, timestamps) values($refereeId, array[${timestamp.toEpochMilli}]) 
       on conflict (referee_id) do update set timestamps = array_prepend(${timestamp.toEpochMilli}, referee_visit.timestamps)
       """

}
