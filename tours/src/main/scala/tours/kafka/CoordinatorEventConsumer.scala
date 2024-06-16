package tours.kafka

import cats.MonadThrow
import cats.implicits._
import fs2.kafka._
import org.typelevel.log4cats.SelfAwareStructuredLogger
import tours.config.KafkaConfig
import tours.database.TourDao
import tours.models.{Hotel, HotelEvent, KafkaEvents, TicketEvent}
import tours.services.errors.ToursServiceError

trait CoordinatorEventConsumer[F[_]] {
  def handleHotelEvent(event: HotelEvent): F[Either[ToursServiceError, Unit]]

  def handleTicketEvent(event: TicketEvent): F[Either[ToursServiceError, Unit]]
}

object CoordinatorEventConsumer {
  def impl[F[_]: MonadThrow](
      dao: TourDao[F],
      kafkaCfg: KafkaConfig
  )(implicit logger: SelfAwareStructuredLogger[F]): CoordinatorEventConsumer[F] = new CoordinatorEventConsumer[F] {

    def handleDeleteHotelEvent(hotelId: Hotel.Id): F[Either[ToursServiceError, Unit]] = {
      val operation = for {
        _ <- dao.deleteHotel(hotelId)
      } yield ()

      operation.attempt.map {
        case Right(_) => Right(())
        case Left(_)  => Left(ToursServiceError.InternalError.default)
      }
    }

    def handleEditHotelEvent(hotelId: Hotel.Id): F[Either[ToursServiceError, Unit]] = ???

    def handleAddHotelEvent(hotel: Hotel): F[Either[ToursServiceError, Unit]] = {
      logger.info("aaaaaaaaaaaaaaaaaaaaaaaa")
      val operation = for {
        _ <- dao.addHotel(hotel)
      } yield ()

      operation.attempt.map {
        case Right(_) => Right(())
        case Left(_) => Left(ToursServiceError.InternalError.default)
      }
    }

    def handleHotelEvent(event: HotelEvent): F[Either[ToursServiceError, Unit]] = {
      val kafkaEvent = KafkaEvents.withNameInsensitive(event.event)
      logger.info("bbbbbbbbbbbbb")
      kafkaEvent match {
        case KafkaEvents.Add =>
          handleAddHotelEvent(event.hotel.get)
        case KafkaEvents.Edit =>
          handleEditHotelEvent(Hotel.Id.fromString(event.hotelId))
        case KafkaEvents.Delete =>
          handleDeleteHotelEvent(Hotel.Id.fromString(event.hotelId))
      }

    }

    def handleTicketEvent(event: TicketEvent): F[Either[ToursServiceError, Unit]] = ???
  }
}
