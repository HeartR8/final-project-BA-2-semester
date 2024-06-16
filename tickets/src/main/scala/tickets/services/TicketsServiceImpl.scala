package tickets.services

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.implicits._
import fs2.kafka.KafkaProducer
import tickets.database.TicketDao
import tickets.models.Ticket
import tickets.services.errors.TicketsServiceError

final case class TicketsServiceImpl[F[_]: MonadThrow](db: TicketDao[F], kafkaProducer: KafkaProducer[F, String, String])
  extends TicketsService[F] {

  override def addTicket(ticket: Ticket): F[Either[TicketsServiceError, Unit]] = {
    val operation = for {
      _ <- db.add(ticket)
    } yield ()

    operation.attempt.map {
      case Right(_) => Right(())
      case Left(_) => Left(TicketsServiceError.InternalError.default)
    }
  }

  override def getTickets: F[Either[TicketsServiceError, NonEmptyList[Ticket]]] = {
    val operation = for {
      tickets <- db.getAll
    } yield tickets

    operation.attempt.map {
      case Right(tickets) => Right(tickets)
      case Left(_) => Left(TicketsServiceError.InternalError.default)
    }
  }

  override def getTicket(id: Ticket.Id): F[Either[TicketsServiceError, Ticket]] = {
    val operation = for {
      ticket <- db.get(id)
    } yield ticket

    operation.attempt.map {
      case Right(ticket) if ticket.nonEmpty => Right(ticket.get)
      case Right(_) => Left(TicketsServiceError.NotFound("Билет не найден"))
      case Left(_) => Left(TicketsServiceError.InternalError.default)
    }
  }

  override def editTicketPrice(
                                     id: Ticket.Id,
                                     price: Double
                                   ): F[Either[TicketsServiceError, Unit]] = {
    val operation = for {
      _ <- db.editPrice(id, price)
    } yield ()

    operation.attempt.map {
      case Right(_) => Right(())
      case Left(_) => Left(TicketsServiceError.InternalError.default)
    }
  }

  override def deleteTicket(
                            id: Ticket.Id
                          ): F[Either[TicketsServiceError, Unit]] = {
    val operation = for {
      _ <- db.delete(id)
    } yield ()

    operation.attempt.map {
      case Right(_) => Right(())
      case Left(_) => Left(TicketsServiceError.InternalError.default)
    }
  }

}

object TicketsServiceImpl {
  def impl[F[_]: MonadThrow](
            dao: TicketDao[F],
            kafkaProducer: KafkaProducer[F, String, String]
          ): TicketsService[F] = new TicketsServiceImpl[F](dao, kafkaProducer)
}
