package tours.kafka

import cats.MonadThrow
import cats.implicits._
import fs2.kafka._
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
  ): CoordinatorEventConsumer[F] = new CoordinatorEventConsumer[F] {

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

    def handleAddHotelEvent(hotelId: Hotel.Id): F[Either[ToursServiceError, Unit]] = ???

    def handleHotelEvent(event: HotelEvent): F[Either[ToursServiceError, Unit]] = {
      val kafkaEvent = KafkaEvents.withNameInsensitive(event.event)
      kafkaEvent match {
        case KafkaEvents.Add =>
          handleAddHotelEvent(Hotel.Id.fromString(event.hotelId))
        case KafkaEvents.Edit =>
          handleEditHotelEvent(Hotel.Id.fromString(event.hotelId))
        case KafkaEvents.Delete =>
          handleDeleteHotelEvent(Hotel.Id.fromString(event.hotelId))
      }

    }

    def handleTicketEvent(event: TicketEvent): F[Either[ToursServiceError, Unit]] = ???
  }
}
