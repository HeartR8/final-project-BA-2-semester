package tickets

import cats.effect._
import cats.implicits._
import com.comcast.ip4s._
import doobie.Transactor
import fs2.kafka.{KafkaProducer, ProducerSettings}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.SwaggerUIOptions
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import tickets.config.AppConfig
import tickets.database.{FlywayMigration, TicketDaoImpl}
import tickets.endpoints.TicketsController
import tickets.services.TicketsServiceImpl

object TicketsApp {

  def run: IO[Unit] = {
    val config = ConfigSource.default.loadOrThrow[AppConfig]

    database.makeTransactor[IO](config.database).use { xa: Transactor[IO] =>
      val kafkaProducerSettings = ProducerSettings[IO, String, String]
        .withBootstrapServers(config.kafka.bootstrapServers)

      val kafkaProducerResource = KafkaProducer.resource(kafkaProducerSettings)

      kafkaProducerResource.use { kafkaProducer =>
        for {
          _ <- FlywayMigration.clear[IO](config.database)
          _ <- FlywayMigration.migrate[IO](config.database)

          ticketDao = TicketDaoImpl.impl[IO](xa)
          ticketsService = TicketsServiceImpl.impl[IO](ticketDao, kafkaProducer)
          ticketsController = TicketsController.impl[IO](ticketsService)

          endpoints = List(
            ticketsController.endpoints
          ).flatten

          swagger = SwaggerInterpreter(
            swaggerUIOptions = SwaggerUIOptions.default.pathPrefix(List("docs", "tickets"))
          ).fromServerEndpoints[IO](
            endpoints = endpoints,
            title = "tickets",
            version = "0.0.1"
          )

          httpApp = Http4sServerInterpreter[IO]().toRoutes(swagger ++ endpoints).orNotFound
          httpAppWithLogging = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

          port <- Sync[IO].fromOption(
            Port.fromInt(config.http.port),
            new RuntimeException("Invalid http port")
          )
          _ <- IO.println(s"Go to http://localhost:${config.http.port}/docs/tickets/#/ to open SwaggerUI")

          _ <- EmberServerBuilder.default[IO]
            .withHost(ipv4"0.0.0.0")
            .withPort(port)
            .withHttpApp(httpAppWithLogging)
            .build
            .useForever
        } yield ()
      }
    }
  }
}
