package tours.database

import cats.effect.kernel.MonadCancelThrow
import doobie.Transactor
import tours.models.Tour

final class TourDaoImpl[F[_] : MonadCancelThrow](xa: Transactor[F]) extends TourDao[F] {
  override def add(tour: Tour): F[Int] = ???
}

object TourDaoImpl {
  def impl[F[_] : MonadCancelThrow](xa: Transactor[F]): TourDaoImpl[F] = new TourDaoImpl[F](xa)
}
