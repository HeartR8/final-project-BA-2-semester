package tours.database

import cats.effect.kernel.MonadCancelThrow
import doobie.Transactor
import doobie.implicits.toSqlInterpolator
import tours.models.{AddTour, Hotel, Tour}
import doobie.implicits._

import java.util.UUID

final class TourDaoImpl[F[_] : MonadCancelThrow](xa: Transactor[F]) extends TourDao[F] {
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

  override def deleteHotel(hotelId: Hotel.Id): F[Int] = (
    sql"DELETE FROM hotels WHERE hotel_id = ${hotelId.toString};" ++
      sql"DELETE FROM tours WHERE hotel_id = ${hotelId.toString}"
    ).update.run.transact(
    xa
  )
}

object TourDaoImpl {
  def impl[F[_] : MonadCancelThrow](xa: Transactor[F]): TourDaoImpl[F] = new TourDaoImpl[F](xa)
}
