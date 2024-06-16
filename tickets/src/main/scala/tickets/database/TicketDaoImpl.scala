package tickets.database

import cats.data.NonEmptyList
import cats.effect.kernel.MonadCancelThrow
import doobie._
import doobie.`enum`.JdbcType
import doobie.implicits._
import doobie.implicits.javasql._
import doobie.postgres._
import doobie.postgres.implicits._
import doobie.postgres.pgisimplicits._
import tickets.models.{KafkaEvents, OutboxEvent, Ticket}

import java.time.Instant
import java.util.UUID

final class TicketDaoImpl[F[_] : MonadCancelThrow](xa: Transactor[F]) extends TicketDao[F] {
  private final implicit val instantPut: Put[Instant] =
    Put.Basic.one(
      JdbcType.TimestampWithTimezone,
      (
        stmt,
        index,
        instantValue
      ) => stmt.setTimestamp(index, java.sql.Timestamp.from(instantValue)),
      (resultSet, index, instantValue) =>
        resultSet.updateTimestamp(index, java.sql.Timestamp.from(instantValue))
    )

  override def add(ticket: Ticket): F[Int] = {
    val departureTime = ticket.departureTime
    val arrivalTime = ticket.arrivalTime
    sql"""INSERT INTO tickets (ticket_id, "from", "to", departure_time, arrival_time, price) VALUES (${ticket.id.toString}, ${ticket.from}, ${ticket.to}, ${departureTime}, ${arrivalTime}, ${ticket.price});
          INSERT INTO tickets_events (ticket_id, event) VALUES (${ticket.id.toString}, ${KafkaEvents.Add.toString});
         """.update.run.transact(
      xa
    )
  }

  override def get(id: Ticket.Id): F[Option[Ticket]] =
    sql"SELECT * from tickets WHERE ticket_id = ${id.toString}".query[Ticket].option.transact(
      xa
    )

  override def getAll: F[NonEmptyList[Ticket]] =
    sql"SELECT * from tickets".query[Ticket].nel.transact(
      xa
    )

  override def editPrice(id: Ticket.Id, price: Double): F[Int] =
    (
      sql"UPDATE tickets SET price = $price WHERE ticket_id = ${id.toString};" ++
        sql"INSERT INTO tickets_events (ticket_id, event) VALUES (${id.toString}, ${KafkaEvents.Edit.toString})"
      ).update.run.transact(
      xa
    )

  override def delete(id: Ticket.Id): F[Int] =
    (sql"DELETE FROM tickets WHERE ticket_id = ${id.toString};" ++
      sql"INSERT INTO tickets_events (ticket_id, event) VALUES (${id.toString}, ${KafkaEvents.Delete.toString})").update.run.transact(
      xa
    )

  override def fetchUnprocessedEvents: F[List[OutboxEvent]] =
    sql"SELECT event_id, ticket_id, event, processed FROM tickets_events WHERE processed = FALSE".query[
      OutboxEvent
    ].to[List].transact(xa)

  override def updateProcessedEvents(eventId: UUID): F[Int] = {
    sql"UPDATE tickets_events SET processed = TRUE WHERE event_id = ${eventId.toString};".update.run.transact(
      xa
    )
  }
}

object TicketDaoImpl {
  def impl[F[_] : MonadCancelThrow](xa: Transactor[F]): TicketDaoImpl[F] = new TicketDaoImpl[F](xa)
}
