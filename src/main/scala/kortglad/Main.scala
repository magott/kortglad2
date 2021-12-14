package kortglad

import bloque.db.Db
import bloque.db.transactional
import bloque.jetty.*
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import bloque.db.Row
import bloque.http.Json
import java.sql.Types
import java.sql.ResultSet
import org.postgresql.util.PGobject
import java.time.OffsetDateTime

given Row[PGobject] = Row
  .jdbc(Types.OTHER, Nil, _.getObject(_), _.setObject(_, _))
  .imap(_.asInstanceOf[PGobject], identity)

given Row[OffsetDateTime] = Row
  .jdbc(Types.TIMESTAMP_WITH_TIMEZONE, Nil, _.getObject(_), _.setObject(_, _))
  .imap(_.asInstanceOf[OffsetDateTime], identity)

def jsonb[A](using j: Json[A]): Row[A] =
  def read(pg: PGobject) = Json.read(pg.getValue, false)
  def write(a: A) =
    val pg = new PGobject
    pg.setType("jsonb")
    pg.setValue(Json.default.write(a))
    pg
  Row[PGobject].imap(read, write)

import scala.util.Using
import java.time.OffsetDateTime

@main def main =
  Using.resource(HikariDataSource()) { ds =>
    ds.setUsername("postgres")
    ds.setPassword("postgres")
    ds.setJdbcUrl("jdbc:postgresql://localhost:5440/postgres")
    val tx = Db.fromDataSource(ds).transactional
    val flyway = Flyway.configure().dataSource(ds).load()
    flyway.migrate()
    Jetty(8080, "./public") {
      App.run(tx)
    }
  }
