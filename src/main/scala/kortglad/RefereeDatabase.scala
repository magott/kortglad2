package kortglad

import bloque.db.Row
import bloque.db.*

import java.time.{OffsetDateTime, Year}

case class DbReferee(
    fiksId: FiksId,
    name: String,
    lastSync: Option[OffsetDateTime]
) derives Row

case class DbSeason(year: Year, matchStats: DbMatchStats) derives Row

case class DbMatchStats(matchStats: List[MatchStat]) derives Row
object DbMatchStats:
  given Row[List[MatchStat]] = jsonb[Map[String, MatchStat]]
    .imap(_.values.toList, _.map(v => (v.fiksId.fiksId.toString -> v)).toMap)

def refereeById(fiksId: FiksId) =
  sql"select fiks_id, name, last_sync from referee where fiks_id = ${fiksId}"
    .query[DbReferee]

def refereeSeasonsByRefereeId(fiksId: FiksId) =
  sql"select year, matches from referee_season where referee_id = ${fiksId}"
    .query[DbSeason]

def updateLastSync(fiksId: FiksId) =
  sql"update referee set last_sync=now() where fiks_id=$fiksId".update

def upsertReferee(fiksId: FiksId, name: String) =
  sql"""
    insert into referee (fiks_id, name) 
    values($fiksId, $name) 
    on conflict (fiks_id) do update set 
    name=excluded.name
  """.update

def upsertMatch(refereeId: FiksId, matchStat: MatchStat) =
  upsertSeason(refereeId, matchStat.year, DbMatchStats(List(matchStat)))

def upsertSeason(refereeId: FiksId, year: Year, matchStats: DbMatchStats) =
  sql"""
       insert into referee_season(referee_id, year, matches) 
       values($refereeId, $year, $matchStats)
       on conflict (referee_id, year) do update set
       matches = referee_season.matches || excluded.matches
       """.update
