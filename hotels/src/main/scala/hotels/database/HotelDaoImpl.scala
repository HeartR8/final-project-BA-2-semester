package hotels.database

import cats.data.NonEmptyList
import cats.effect.kernel.MonadCancelThrow
import doobie.Transactor
import doobie.implicits._
import hotels.models.{Hotel, KafkaEvents}

final class HotelDaoImpl[F[_]: MonadCancelThrow](xa: Transactor[F]) extends HotelDao[F] {

  override def add(hotel: Hotel): F[Int] =
    (sql"INSERT INTO hotels (hotel_id, name, description, price_per_night) " ++
      sql"VALUES (${hotel.id.toString}, ${hotel.name}, ${hotel.description}, ${hotel.pricePerNight});" ++
      sql"INSERT INTO hotels_events (hotel_id, event)" ++
      sql"VALUES (${hotel.id.toString}, ${KafkaEvents.Add.toString})").update.run.transact(
      xa
    )

  override def get(id: Hotel.Id): F[Option[Hotel]] =
    sql"SELECT * from hotels WHERE hotel_id = ${id.toString}".query[Hotel].option.transact(
      xa
    )

  override def getAll: F[NonEmptyList[Hotel]] = sql"SELECT * from hotels".query[Hotel].nel.transact(
    xa
  )

  override def editDescription(id: Hotel.Id, description: String): F[Int] =
    (sql"UPDATE hotels SET description = $description WHERE hotel_id = ${id.toString};" ++
      sql"INSERT INTO hotels_events (hotel_id, event)" ++
      sql"VALUES (${id.toString}, ${KafkaEvents.Edit.toString})").update.run.transact(
      xa
    )

  override def delete(id: Hotel.Id): F[Int] =
    (sql"DELETE FROM hotels WHERE hotel_id = ${id.toString};" ++
      sql"INSERT INTO hotels_events (hotel_id, event)" ++
      sql"VALUES (${id.toString}, ${KafkaEvents.Delete.toString})").update.run.transact(
      xa
    )
}

object HotelDaoImpl {
  def impl[F[_]: MonadCancelThrow](xa: Transactor[F]): HotelDaoImpl[F] = new HotelDaoImpl[F](xa)
}
