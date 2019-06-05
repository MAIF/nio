package loader

import javax.sql.DataSource
import liquibase.database.Database
import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import play.Logger
import play.api.Configuration
import scalikejdbc.async.{AsyncConnectionPool, AsyncConnectionPoolSettings}

case class DbLoader(database: DataSource, config: Configuration) {

  private val connection = database.getConnection

  Logger.info("Initializing database connection pool")

  AsyncConnectionPool.singleton(
    config.get[String]("db.default.url"),
    config.get[String]("db.default.username"),
    config.get[String]("db.default.password"),
    AsyncConnectionPoolSettings(
      config.get[Int]("db.default.maxPoolSize"),
      config.get[Int]("db.default.maxPoolSize"),
      config.get[Int]("db.default.maxIdleMillis"),
    )
  )
  Logger.info("Initializing run scripts to create tables, indexes...")
  try {
    val db: Database = DatabaseFactory
      .getInstance()
      .findCorrectDatabaseImplementation(new JdbcConnection(connection))
    val liquibase =
      new Liquibase("db_changes.sql", new ClassLoaderResourceAccessor(), db)
    liquibase.update(new Contexts(), new LabelExpression())
  } catch {
    case e: Exception =>
      Logger.error("Error initializing db", e)
      throw new RuntimeException(e)
  } finally if (connection != null) connection.close()
}
