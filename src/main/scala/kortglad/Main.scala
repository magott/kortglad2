package kortglad

import bloque.db.Db
import bloque.db.transactional
import bloque.jetty.*
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway

import scala.util.Using

@main def main = Jetty(8080, "./public") {
  Using.resource(HikariDataSource()) { ds =>
    ds.setUsername("postgres")
    ds.setPassword("postgres")
    ds.setJdbcUrl("jdbc:postgresql://localhost:5440/postgres")
    val tx = Db.fromDataSource(ds).transactional
    val flyway = Flyway.configure().dataSource(ds).load()
    flyway.migrate()
    App.run(tx)
  }
}
