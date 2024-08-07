package tours.services

import cats.MonadThrow
import cats.implicits._
import tours.database.TourDao
import tours.models.{AddTour, Tour}
import tours.services.errors.ToursServiceError

final case class TourServiceImpl[F[_] : MonadThrow](
                                                     db: TourDao[F]
                                                   ) extends TourService[F] {
  override def addTour(tour: AddTour): F[Either[ToursServiceError, Unit]] = {
    val operation = for {
      _ <- db.add(tour)
    } yield ()

    operation.attempt.map {
      case Right(_) => Right(())
      case Left(_) => Left(ToursServiceError.InternalError.default)
    }
  }

  override def bookTour(tourId: Tour.Id): F[Either[ToursServiceError, Unit]] = {
    val operation = for {
      _ <- db.book(tourId)
    } yield ()

    operation.attempt.map {
      case Right(_) => Right(())
      case Left(_) => Left(ToursServiceError.InternalError.default)
    }
  }

}

object TourServiceImpl {
  def impl[F[_] : MonadThrow](
                               dao: TourDao[F]
                             ): TourService[F] = new TourServiceImpl[F](dao)
}
