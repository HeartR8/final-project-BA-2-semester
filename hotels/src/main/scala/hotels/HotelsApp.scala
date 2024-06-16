package hotels

import cats.effect._
import cats.implicits._
import com.comcast.ip4s._
import doobie.Transactor
import fs2.kafka.{KafkaProducer, ProducerSettings}
import hotels.config.AppConfig
import hotels.database.{FlywayMigration, HotelDaoImpl}
import hotels.endpoints.HotelsController
import hotels.kafka.EventScheduler
import hotels.services.HotelsServiceImpl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.SwaggerUIOptions
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object HotelsApp {

  def run: IO[Unit] = {
    val config = ConfigSource.default.loadOrThrow[AppConfig]

    implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

    database.makeTransactor[IO](config.database).use { xa: Transactor[IO] =>
      val kafkaProducerSettings = ProducerSettings[IO, String, String]
        .withBootstrapServers(config.kafka.bootstrapServers)

      val kafkaProducerResource = KafkaProducer.resource(kafkaProducerSettings)

      kafkaProducerResource.use { kafkaProducer =>
        for {
          _ <- FlywayMigration.clear[IO](config.database)
          _ <- FlywayMigration.migrate[IO](config.database)

          hotelDao = HotelDaoImpl.impl[IO](xa)
          hotelsService = HotelsServiceImpl.impl[IO](hotelDao, kafkaProducer, config.kafka.topic)
          hotelsController = HotelsController.impl[IO](hotelsService)

          _ <- EventScheduler.impl(
            hotelDao,
            kafkaProducer,
            config.kafka.topic
          ).processOutboxEvents.start

          endpoints = List(
            hotelsController.endpoints
          ).flatten

          swagger = SwaggerInterpreter(
            swaggerUIOptions = SwaggerUIOptions.default.pathPrefix(List("docs", "hotels"))
          ).fromServerEndpoints[IO](
            endpoints = endpoints,
            title = "hotels",
            version = "0.0.1"
          )

          httpApp = Http4sServerInterpreter[IO]().toRoutes(swagger ++ endpoints).orNotFound
          httpAppWithLogging = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

          port <- Sync[IO].fromOption(
            Port.fromInt(config.http.port),
            new RuntimeException("Invalid http port")
          )
          _ <- IO.println(
            s"Go to http://localhost:${config.http.port}/docs/hotels/#/ to open SwaggerUI"
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
