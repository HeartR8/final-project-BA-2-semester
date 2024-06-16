package tours.services

import cats.MonadThrow
import cats.implicits._
import fs2.kafka.KafkaProducer
import tours.database.TourDao
import tours.models.Tour
import tours.services.errors.ToursServiceError

final case class TourServiceImpl[F[_] : MonadThrow](db: TourDao[F], kafkaProducer: KafkaProducer[F, String, String])
  extends TourService[F] {
  override def addTour(tour: Tour): F[Either[ToursServiceError, Unit]] = ???

  override def bookTour(tour: Tour): F[Either[ToursServiceError, Unit]] = ???

}

object TourServiceImpl {
  def impl[F[_] : MonadThrow](
                               dao: TourDao[F],
                               kafkaProducer: KafkaProducer[F, String, String]
                             ): TourService[F] = new TourServiceImpl[F](dao, kafkaProducer)
}
