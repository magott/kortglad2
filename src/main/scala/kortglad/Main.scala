package kortglad

import bloque.db.*
import bloque.pg.Pg
import bloque.jetty.*
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.flywaydb.core.Flyway

import java.sql.Types
import java.sql.ResultSet

import java.time.OffsetDateTime
import scala.util.Properties

//given Row[PGobject] = Row
//  .jdbc(Types.OTHER, Nil, _.getObject(_), _.setObject(_, _))
//  .imap(_.asInstanceOf[PGobject], identity)

//given Row[OffsetDateTime] = Row
//  .jdbc(
//    Types.TIMESTAMP_WITH_TIMEZONE,
//    Nil,
//    _.getObject(_, classOf[OffsetDateTime]),
//    _.setObject(_, _)
//  )

//def jsonb[A](using j: Json[A]): Row[A] =
//  def read(pg: PGobject) = Json.read(pg.getValue, false)
//  def write(a: A) =
//    val pg = new PGobject
//    pg.setType("jsonb")
//    pg.setValue(Json.default.write(a))
//    pg
//  Row[PGobject].imap(read, write)

import scala.util.Using
import java.time.OffsetDateTime

@main def main =
  val hikariConfig = Properties
    .envOrNone("DATABASE_URL")
    .map(databaseUrlToHikariConfig)
    .getOrElse {
      val config = HikariConfig()
      config.setJdbcUrl("jdbc:postgresql://localhost:5440/postgres")
      config.setUsername("postgres")
      config.setPassword("postgres")
      config
    }

  Using.resource(HikariDataSource(hikariConfig)) { ds =>
    val tx = Sessions.fromDataSource(ds)
    val flyway = Flyway.configure().dataSource(ds).load()
    flyway.migrate()
    Jobs.TournamentScraperJob.schedule(tx)
    Jobs.SingleMatchScraperJob.schedule(tx)
    Jobs.RefereeRefresherJob.schedule(tx)
    val port = Properties.envOrElse("PORT", "8080").toInt
    Jetty(port, "./public") {
      App.run(tx)
    }
  }

def databaseUrlToHikariConfig(dbUriString: String) =
  val dbUri = java.net.URI.create(dbUriString)
  val username = dbUri.getUserInfo.split(":")(0)
  val password = dbUri.getUserInfo.split(":")(1)
  val jdbcUrl =
    s"jdbc:postgresql://${dbUri.getHost}:${dbUri.getPort}${dbUri.getPath}?sslmode=require"
  val config = HikariConfig()
  config.setJdbcUrl(jdbcUrl)
  config.setUsername(username)
  config.setPassword(password)
  config
