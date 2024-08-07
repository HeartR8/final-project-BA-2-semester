package tours.kafka

import cats.MonadThrow
import cats.implicits._
import fs2.kafka._
import org.typelevel.log4cats.SelfAwareStructuredLogger
import tours.config.KafkaConfig
import tours.database.TourDao
import tours.models._
import tours.services.errors.ToursServiceError

trait CoordinatorEventConsumer[F[_]] {
  def handleHotelEvent(hotelId: Hotel.Id, event: HotelEvent): F[Either[ToursServiceError, Unit]]

  def handleTicketEvent(ticketId: Ticket.Id, event: TicketEvent): F[Either[ToursServiceError, Unit]]
}

object CoordinatorEventConsumer {
  def impl[F[_]: MonadThrow](
      dao: TourDao[F],
      kafkaCfg: KafkaConfig
  )(implicit logger: SelfAwareStructuredLogger[F]): CoordinatorEventConsumer[F] =
    new CoordinatorEventConsumer[F] {

      def handleDeleteHotelEvent(hotelId: Hotel.Id): F[Either[ToursServiceError, Unit]] = {
        val operation = for {
          _ <- dao.deleteHotel(hotelId)
        } yield ()

        operation.attempt.map {
          case Right(_) => Right(())
          case Left(_)  => Left(ToursServiceError.InternalError.default)
        }
      }

      def handleEditHotelEvent(hotel: Hotel): F[Either[ToursServiceError, Unit]] = {
        val operation = for {
          _ <- dao.editHotel(hotel)
        } yield ()

        operation.attempt.map {
          case Right(_) => Right(())
          case Left(_) => Left(ToursServiceError.InternalError.default)
        }
      }

      def handleAddHotelEvent(hotel: Hotel): F[Either[ToursServiceError, Unit]] = {
        val operation = for {
          _ <- dao.addHotel(hotel)
        } yield ()

        operation.attempt.map {
          case Right(_) => Right(())
          case Left(_)  => Left(ToursServiceError.InternalError.default)
        }
      }

      def handleDeleteTicketEvent(hotelId: Ticket.Id): F[Either[ToursServiceError, Unit]] = {
        val operation = for {
          _ <- dao.deleteTicket(hotelId)
        } yield ()

        operation.attempt.map {
          case Right(_) => Right(())
          case Left(_) => Left(ToursServiceError.InternalError.default)
        }
      }

      def handleEditTicketEvent(hotel: Ticket): F[Either[ToursServiceError, Unit]] = {
        val operation = for {
          _ <- dao.editTicket(hotel)
        } yield ()

        operation.attempt.map {
          case Right(_) => Right(())
          case Left(_) => Left(ToursServiceError.InternalError.default)
        }
      }

      def handleAddTicketEvent(hotel: Ticket): F[Either[ToursServiceError, Unit]] = {
        val operation = for {
          _ <- dao.addTicket(hotel)
        } yield ()

        operation.attempt.map {
          case Right(_) => Right(())
          case Left(_) => Left(ToursServiceError.InternalError.default)
        }
      }

      def handleHotelEvent(
          hotelId: Hotel.Id,
          event: HotelEvent
      ): F[Either[ToursServiceError, Unit]] = {
        val kafkaEvent = KafkaEvents.withNameInsensitive(event.event)
        kafkaEvent match {
          case KafkaEvents.Add =>
            handleAddHotelEvent(event.hotel.get)
          case KafkaEvents.Edit =>
            handleEditHotelEvent(event.hotel.get)
          case KafkaEvents.Delete =>
            handleDeleteHotelEvent(hotelId)
        }

      }

      def handleTicketEvent(ticketId: Ticket.Id, event: TicketEvent): F[Either[ToursServiceError, Unit]] = {
        val kafkaEvent = KafkaEvents.withNameInsensitive(event.event)
        kafkaEvent match {
          case KafkaEvents.Add =>
            handleAddTicketEvent(event.ticket.get)
          case KafkaEvents.Edit =>
            handleEditTicketEvent(event.ticket.get)
          case KafkaEvents.Delete =>
            handleDeleteTicketEvent(ticketId)
        }
      }
    }
}
