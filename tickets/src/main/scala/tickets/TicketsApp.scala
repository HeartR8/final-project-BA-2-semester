package tickets

import cats.effect._
import cats.implicits._
import com.comcast.ip4s._
import doobie.Transactor
import fs2.kafka.{KafkaProducer, ProducerSettings}
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import sttp.tapir.swagger.SwaggerUIOptions
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import tickets.config.AppConfig
import tickets.database.{FlywayMigration, TicketDaoImpl}
import tickets.endpoints.TicketsController
import tickets.kafka.EventScheduler
import tickets.services.TicketsServiceImpl
import tickets.metrics.ServerMetrics

object TicketsApp {

  def run: IO[Unit] = {
    val config = ConfigSource.default.loadOrThrow[AppConfig]

    implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

    database.makeTransactor[IO](config.database).use { xa: Transactor[IO] =>
      val kafkaProducerSettings = ProducerSettings[IO, String, String]
        .withBootstrapServers(config.kafka.bootstrapServers)

      val kafkaProducerResource = KafkaProducer.resource(kafkaProducerSettings)

      val prometheusRegistry = PrometheusRegistry.defaultRegistry
      val prometheusMetrics = ServerMetrics.register[IO](prometheusRegistry)

      kafkaProducerResource.use { kafkaProducer =>
        for {
          _ <- FlywayMigration.clear[IO](config.database)
          _ <- FlywayMigration.migrate[IO](config.database)

          ticketDao = TicketDaoImpl.impl[IO](xa)
          ticketsService = TicketsServiceImpl.impl[IO](ticketDao, kafkaProducer)
          ticketsController = TicketsController.impl[IO](ticketsService)

          _ <- EventScheduler.impl(
            ticketDao,
            kafkaProducer,
            config.kafka.topic
          ).processOutboxEvents.start

          endpoints = List(
            ticketsController.endpoints,
            List(prometheusMetrics.metricsEndpoint)
          ).flatten

          swagger = SwaggerInterpreter(
            swaggerUIOptions = SwaggerUIOptions.default.pathPrefix(List("docs", "tickets"))
          ).fromServerEndpoints[IO](
            endpoints = endpoints,
            title = "tickets",
            version = "0.0.1"
          )

          serverOptions = Http4sServerOptions.customiseInterceptors[IO]
            .metricsInterceptor(
              prometheusMetrics.metricsInterceptor(ignoreEndpoints = swagger.map(_.endpoint))
            )
            .options

          httpApp =
            Http4sServerInterpreter[IO](serverOptions).toRoutes(swagger ++ endpoints).orNotFound
          httpAppWithLogging = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

          port <- Sync[IO].fromOption(
            Port.fromInt(config.http.port),
            new RuntimeException("Invalid http port")
          )
          _ <- IO.println(
            s"Go to http://localhost:${config.http.port}/docs/tickets/#/ to open SwaggerUI"
          )

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
