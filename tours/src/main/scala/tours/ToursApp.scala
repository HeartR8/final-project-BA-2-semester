package tours

import cats.effect._
import cats.implicits._
import com.comcast.ip4s._
import doobie.Transactor
import fs2.kafka.{AutoOffsetReset, ConsumerSettings, KafkaConsumer, KafkaProducer, ProducerSettings}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.SwaggerUIOptions
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import tours.config.AppConfig
import tours.database.{FlywayMigration, TourDaoImpl}
import tours.endpoints.ToursController
import tours.kafka.CoordinatorEventConsumer
import tours.models.{HotelEvent, TicketEvent}
import tours.services.TourServiceImpl

object ToursApp {

  def run: IO[Unit] = {
    val config = ConfigSource.default.loadOrThrow[AppConfig]
    implicit val logger = Slf4jLogger.getLogger[IO]

    database.makeTransactor[IO](config.database).use { xa: Transactor[IO] =>
      val kafkaProducerSettings = ProducerSettings[IO, String, String]
        .withBootstrapServers(config.kafka.bootstrapServers)

      val kafkaConsumerSettings = ConsumerSettings[IO, String, String]
        .withBootstrapServers(config.kafka.bootstrapServers)
        .withGroupId(config.kafka.consumerGroupId)
        .withAutoOffsetReset(AutoOffsetReset.Latest)

      val kafkaConsumerResource = KafkaConsumer.resource(kafkaConsumerSettings)
      val kafkaProducerResource = KafkaProducer.resource(kafkaProducerSettings)

      kafkaProducerResource.use { kafkaProducer =>
        kafkaConsumerResource.use { kafkaHotelsConsumer =>
          kafkaConsumerResource.use { kafkaTicketsConsumer =>
            for {
              _ <- FlywayMigration.clear[IO](config.database)
              _ <- FlywayMigration.migrate[IO](config.database)

              tourDao = TourDaoImpl.impl[IO](xa)
              tourService = TourServiceImpl.impl[IO](tourDao, kafkaProducer)
              tourController = ToursController.impl[IO](tourService)
              coordinatorEventConsumer = CoordinatorEventConsumer.impl[IO](tourDao, config.kafka)

              _ <- kafkaHotelsConsumer.subscribeTo(config.kafka.hotelsTopic)
              consumerHotelsStream = kafkaHotelsConsumer.stream.evalMap { committable =>
                val record = committable.record
                logger.info(s"Consumed record with key: ${record.key}, value: ${record.value}") *>
                  coordinatorEventConsumer.handleHotelEvent(
                    HotelEvent(record.key, record.value)
                  ).handleErrorWith { error =>
                    logger.error(error)(
                      s"Failed to process record with key: ${record.key}, value: ${record.value}"
                    )
                  }
              }.compile.drain

              _ <- kafkaTicketsConsumer.subscribeTo(config.kafka.ticketsTopic)
              consumerTicketsStream = kafkaTicketsConsumer.stream.evalMap { committable =>
                val record = committable.record
                logger.info(s"Consumed record with key: ${record.key}, value: ${record.value}") *>
                  coordinatorEventConsumer.handleTicketEvent(
                    TicketEvent(record.key, record.value)
                  ).handleErrorWith { error =>
                    logger.error(error)(
                      s"Failed to process payment event with key: ${record.key}, value: ${record.value}"
                    )
                  }
              }.compile.drain

              _ <- consumerHotelsStream.start
              _ <- consumerTicketsStream.start

              endpoints = List(
                tourController.endpoints
              ).flatten

              swagger = SwaggerInterpreter(
                swaggerUIOptions = SwaggerUIOptions.default.pathPrefix(List("docs", "tour"))
              ).fromServerEndpoints[IO](
                endpoints = endpoints,
                title = "tour",
                version = "0.0.1"
              )

              httpApp = Http4sServerInterpreter[IO]().toRoutes(swagger ++ endpoints).orNotFound
              httpAppWithLogging = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

              port <- Sync[IO].fromOption(
                Port.fromInt(config.http.port),
                new RuntimeException("Invalid http port")
              )
              _ <-
                IO.println(
                  s"Go to http://localhost:${config.http.port}/docs/tour/#/ to open SwaggerUI"
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
  }
}
