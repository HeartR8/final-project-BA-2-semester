package hotels.services

import cats.data.NonEmptyList
import hotels.models.Hotel
import hotels.services.errors.HotelsServiceError

trait HotelsService[F[_]] {
  def addHotel(hotel: Hotel): F[Either[HotelsServiceError, Unit]]

  def getHotels: F[Either[
    HotelsServiceError,
    NonEmptyList[Hotel]
  ]]

  def getHotel(id: Hotel.Id): F[Either[HotelsServiceError, Hotel]]

  def editHotelDescription(
      id: Hotel.Id,
      description: String
  ): F[Either[HotelsServiceError, Unit]]

  def deleteHotel(id: Hotel.Id): F[Either[HotelsServiceError, Unit]]
}
