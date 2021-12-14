package kortglad

import bloque.db.Row
import bloque.db.*

import java.time.Year

case class DbReferee(fiksId: FiksId, name: String) derives Row
case class DbSeason(year: Year, matchStats: List[MatchStat])

def refereeById(fiksId: FiksId) =
  sql"select fiks_id, name from referee where fiks_id = ${fiksId.fiksId}"
    .query[DbReferee]

//def refereeSeasonByRefereeId(fiksId: FiksId) =
//  sql"select referee_id, year, matches from referee_season where referee_id = ${fiksId.fiksId}"
//    .query[DbSeason]

def insertReferee(name: String, fiksId: FiksId) =
  sql"insert into referee (fiks_id, name) values($fiksId, $name)".update
