package tours.database

import cats.effect.kernel.MonadCancelThrow
import doobie.`enum`.JdbcType
import doobie.{Get, Put, Transactor}
import doobie.implicits.toSqlInterpolator
import tours.models.{AddTour, Hotel, Ticket, Tour}
import doobie.implicits._

import java.time.Instant
import java.util.UUID

final class TourDaoImpl[F[_] : MonadCancelThrow](xa: Transactor[F]) extends TourDao[F] {
  implicit val uuidGet: Get[UUID] = Get[String].map(UUID.fromString)

  private implicit val instantPut: Put[Instant] =
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

  override def add(tour: AddTour): F[Int] = {
    sql"""INSERT INTO tours (tour_id, hotel_id, ticket_to_id, ticket_from_id) VALUES (${UUID.randomUUID().toString}, ${tour.hotelId.toString}, ${tour.ticketToId.toString}, ${tour.ticketFromId.toString});""".update.run.transact(
      xa
    )
  }

  override def book(tourId: Tour.Id): F[Int] = {
    sql"""UPDATE tours SET is_booked = true WHERE tour_id = ${tourId.toString};""".update.run.transact(
      xa
    )
  }

  override def addHotel(hotel: Hotel): F[Int] = {
    sql"INSERT INTO hotels (hotel_id, name, description, price_per_night) VALUES (${hotel.id.toString}, ${hotel.name}, ${hotel.description}, ${hotel.pricePerNight});".update.run.transact(
      xa
    )
  }

  def editHotel(hotel: Hotel): F[Int] = {
    sql"UPDATE hotels SET name = ${hotel.name}, price_per_night = ${hotel.pricePerNight}, description = ${hotel.description} WHERE hotel_id = ${hotel.id.toString};"
      .update.run.transact(xa)
  }

  override def deleteHotel(hotelId: Hotel.Id): F[Int] = (
    sql"DELETE FROM hotels WHERE hotel_id = ${hotelId.toString};" ++
      sql"DELETE FROM tours WHERE hotel_id = ${hotelId.toString}"
    ).update.run.transact(
    xa
  )

  override def addTicket(ticket: Ticket): F[Int] = {
    val departureTime = ticket.departureTime
    val arrivalTime = ticket.arrivalTime
    sql"""INSERT INTO tickets (ticket_id, "from", "to", departure_time, arrival_time, price) VALUES (${ticket.id.toString}, ${ticket.from}, ${ticket.to}, ${departureTime}, ${arrivalTime}, ${ticket.price});
           """.update.run.transact(
      xa
    )
  }

  override def editTicket(ticket: Ticket): F[Int] = {
    sql"""UPDATE tickets SET "from" = ${ticket.from}, "to" = ${ticket.to}, departure_time = ${ticket.departureTime}, arrival_time = ${ticket.arrivalTime}, price = ${ticket.price} WHERE ticket_id = ${ticket.id.toString};"""
      .update.run.transact(xa)
  }

  override def deleteTicket(ticketId: Ticket.Id): F[Int] = (
    sql"DELETE FROM tickets WHERE ticket_id = ${ticketId.toString};" ++
      sql"DELETE FROM tours WHERE ticket_from_id = ${ticketId.toString} OR ticket_to_id = ${ticketId.toString}"
    ).update.run.transact(
    xa
  )
}

object TourDaoImpl {
  def impl[F[_] : MonadCancelThrow](xa: Transactor[F]): TourDaoImpl[F] = new TourDaoImpl[F](xa)
}
