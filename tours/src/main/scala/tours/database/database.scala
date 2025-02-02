package tours

import cats.effect.{Async, Resource}
import cats.implicits.catsSyntaxOptionId
import doobie.hikari.{Config, HikariTransactor}
import tours.config.PostgresConfig

package object database {

  def makeTransactor[F[_]: Async](config: PostgresConfig): Resource[F, HikariTransactor[F]] = {
    val hikariConfig = Config(
      jdbcUrl = config.url,
      username = config.user.some,
      password = config.password.some,
      maximumPoolSize = config.poolSize,
      driverClassName = "org.postgresql.Driver".some
    )

    HikariTransactor.fromConfig[F](hikariConfig)
  }

}
