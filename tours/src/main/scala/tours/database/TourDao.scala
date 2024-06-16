package tours.database

import tours.models.{AddTour, Hotel, Tour}

trait TourDao[F[_]] {
  def add(tour: AddTour): F[Int]
  def book(tourId: Tour.Id): F[Int]

  def addHotel(hotel: Hotel): F[Int]

  def editHotel(hotel: Hotel): F[Int]

  def deleteHotel(hotelId: Hotel.Id): F[Int]
}
