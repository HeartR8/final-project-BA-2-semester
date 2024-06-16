package tours.database

import tours.models.{AddTour, Tour}

trait TourDao[F[_]] {
  def add(tour: AddTour): F[Int]
  def book(tourId: Tour.Id): F[Int]

}
