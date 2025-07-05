package kortglad

import bloque.db.*
import bloque.pg.Pg
import java.time.{OffsetDateTime, Year}

case class DbReferee(
    fiksId: FiksId,
    name: String,
    lastSync: Option[OffsetDateTime]
) derives Db

case class DbSeason(year: Year, matchStats: DbMatchStats) derives Db

case class DbMatchStats(matchStats: List[MatchStat]) derives Db
object DbMatchStats:
  given Db[List[MatchStat]] = Pg
    .jsonb[Map[String, MatchStat]]
    .imap(_.values.toList, _.map(v => (v.fiksId.fiksId.toString -> v)).toMap)

def refereeById(fiksId: FiksId) =
  sql"select fiks_id, name, last_sync from referee where fiks_id = ${fiksId}"
    .query[DbReferee]

def refereeSeasonsByRefereeId(fiksId: FiksId) =
  sql"select year, matches from referee_season where referee_id = ${fiksId}"
    .query[DbSeason]

def updateLastSync(fiksId: FiksId) =
  sql"update referee set last_sync=now() where fiks_id=$fiksId"

def activateReferee(fiksId: FiksId) =
  sql"update referee set active = true where active = false and fiks_id=$fiksId"

def upsertReferee(fiksId: FiksId, name: String) =
  sql"""
    insert into referee (fiks_id, name) 
    values($fiksId, $name) 
    on conflict (fiks_id) do update set 
    name=excluded.name
  """

def searchReferees(search: String) =
  val term = search
    .trim()
    .filterNot(_ == ':')
    .split("\\s+")
    .map(_ + ":*")
    .mkString(" & ")
//  println(term)
  sql"""
    select fiks_id, name from referee
    where to_tsquery('simple', $term) @@ name_tsv
     """.query[IndexedReferee]

def upsertMatch(refereeId: FiksId, matchStat: MatchStat) =
  upsertSeason(refereeId, matchStat.year, DbMatchStats(List(matchStat)))

def upsertSeason(refereeId: FiksId, year: Year, matchStats: DbMatchStats) =
  sql"""
       insert into referee_season(referee_id, year, matches) 
       values($refereeId, $year, $matchStats)
       on conflict (referee_id, year) do update set
       matches = referee_season.matches || excluded.matches
       """
