package kortglad

import bloque.db.*
import bloque.pg.Pg
import bloque.jetty.*
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory

import java.sql.Types
import java.sql.ResultSet
import java.time.OffsetDateTime
import scala.util.Properties
import scala.util.Using
import java.time.OffsetDateTime
import java.util.TimeZone

val logger = LoggerFactory.getLogger("Main")

@main def main =
  TimeZone.setDefault(TimeZone.getTimeZone("Europe/Oslo"))
  Properties
    .envOrNone("deploy_target")
    .foreach(target => logger.info(s"""Starting app for ${target}"""))
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
  val flyIOAppName = Properties.envOrNone("deploy_target")
  val sslMode = flyIOAppName
    .filter(target => target == "fly_io")
    .map(_ => "")
    .getOrElse("?sslmode=require")
  val username = dbUri.getUserInfo.split(":")(0)
  val password = dbUri.getUserInfo.split(":")(1)
  val jdbcUrl =
    s"jdbc:postgresql://${dbUri.getHost}:${dbUri.getPort}${dbUri.getPath}${sslMode}"
  val config = HikariConfig()
  config.setJdbcUrl(jdbcUrl)
  config.setUsername(username)
  config.setPassword(password)
  config
