package tours.services

import cats.MonadThrow
import cats.implicits._
import fs2.kafka.KafkaProducer
import tours.database.TourDao
import tours.models.{AddTour, Tour}
import tours.services.errors.ToursServiceError

final case class TourServiceImpl[F[_]: MonadThrow](
    db: TourDao[F],
    kafkaProducer: KafkaProducer[F, String, String]
) extends TourService[F] {
  override def addTour(tour: AddTour): F[Either[ToursServiceError, Unit]] = {
    val operation = for {
      _ <- db.add(tour)
    } yield ()

    operation.attempt.map {
      case Right(_) => Right(())
      case Left(e)  => Left(ToursServiceError.InternalError.default)
    }
  }

  override def bookTour(tourId: Tour.Id): F[Either[ToursServiceError, Unit]] = {
    val operation = for {
      _ <- db.book(tourId)
    } yield ()

    operation.attempt.map {
      case Right(_) => Right(())
      case Left(e)  => Left(ToursServiceError.InternalError.default)
    }
  }

}

object TourServiceImpl {
  def impl[F[_]: MonadThrow](
      dao: TourDao[F],
      kafkaProducer: KafkaProducer[F, String, String]
  ): TourService[F] = new TourServiceImpl[F](dao, kafkaProducer)
}
