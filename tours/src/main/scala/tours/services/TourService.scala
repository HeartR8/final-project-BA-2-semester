package tours.services

import tours.models.{AddTour, Tour}
import tours.services.errors.ToursServiceError

trait TourService[F[_]] {
  def addTour(tour: AddTour): F[Either[ToursServiceError, Unit]]
  def bookTour(tourId: Tour.Id): F[Either[ToursServiceError, Unit]]
}
