package tours.database

import tours.models.Tour

trait TourDao[F[_]] {
  def add(tour: Tour): F[Int]
}
