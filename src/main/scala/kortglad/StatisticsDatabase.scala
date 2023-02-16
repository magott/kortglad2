package kortglad

import bloque.db.{Connections, sql}

import java.time.Instant
class StatisticsDatabase(val db: Connections) {

  def addVisit(refereeId: FiksId, timestamp: Instant) = db.tx {
    addVisitSql(refereeId, timestamp).update
  }

  def lastVisited = lastVisitedRefereesSql(10).query[Statistics.LastVisited]

  private def addVisitSql(refereeId: FiksId, timestamp: Instant) =
    sql"""
       insert into referee_visit(referee_id, timestamps) values($refereeId, array[${timestamp.toEpochMilli}]) 
       on conflict (referee_id) do update set timestamps = array_prepend(${timestamp.toEpochMilli}, referee_visit.timestamps)
       """

  private def lastVisitedRefereesSql(limit: Int) =
    sql"""
         select referee_id, name, timestamps[1] as last_visit
         from referee_visit
         join referee r on referee_visit.referee_id = r.fiks_id
         order by last_visit desc
         limit $limit
       """

  private def mostVisitedRefereesSql(limit: Int) =
    sql"""
         select referee_id, name, total_visit
         from referee_visit
         join referee r on referee_visit.referee_id = r.fiks_id
         order by total_visit desc
         limit $limit
       """

  private def searchesByRefereeSince(instant: Instant) =
    sql"""
        select rv.referee_id, count(v.ts)
        from referee_visit rv
        join (select referee_id, unnest(timestamps) ts from referee_visit) as v on v.referee_id=rv.referee_id
        where ts > ${instant.toEpochMilli}
        group by rv.referee_id
       """

  private def searchesSince(instant: Instant) =
    sql"""
      select count(v.ts)
      from (select unnest(timestamps) ts from referee_visit) v
      where ts > ${instant.toEpochMilli}
     """
}
