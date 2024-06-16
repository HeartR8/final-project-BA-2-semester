package hotels.database

import cats.data.NonEmptyList
import hotels.models.Hotel

trait HotelDao[F[_]] {
  def add(Hotel: Hotel): F[Int]
  def get(id: Hotel.Id): F[Option[Hotel]]
  def getAll: F[NonEmptyList[Hotel]]
  def editDescription(id: Hotel.Id, description: String): F[Int]
  def delete(id: Hotel.Id): F[Int]
}
