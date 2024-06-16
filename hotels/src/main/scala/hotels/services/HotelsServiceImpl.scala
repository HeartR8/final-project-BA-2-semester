package hotels.services

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.implicits._
import fs2.kafka.KafkaProducer
import hotels.database.HotelDao
import hotels.models.Hotel
import hotels.services.errors.HotelsServiceError

final case class HotelsServiceImpl[F[_]: MonadThrow](
    db: HotelDao[F],
    kafkaProducer: KafkaProducer[F, String, String],
    topic: String
) extends HotelsService[F] {

  override def addHotel(hotel: Hotel): F[Either[HotelsServiceError, Unit]] = {
    val operation = for {
      _ <- db.add(hotel)
    } yield ()

    operation.attempt.map {
      case Right(_) => Right(())
      case Left(_)  => Left(HotelsServiceError.InternalError.default)
    }
  }

  override def getHotels: F[Either[HotelsServiceError, NonEmptyList[Hotel]]] = {
    val operation = for {
      hotels <- db.getAll
    } yield hotels

    operation.attempt.map {
      case Right(hotels) => Right(hotels)
      case Left(_)       => Left(HotelsServiceError.InternalError.default)
    }
  }

  override def getHotel(id: Hotel.Id): F[Either[HotelsServiceError, Hotel]] = {
    val operation = for {
      hotel <- db.get(id)
    } yield hotel

    operation.attempt.map {
      case Right(hotel) if hotel.nonEmpty => Right(hotel.get)
      case Right(_)                       => Left(HotelsServiceError.NotFound("Отель не найден"))
      case Left(_)                        => Left(HotelsServiceError.InternalError.default)
    }
  }

  override def editHotelDescription(
      id: Hotel.Id,
      description: String
  ): F[Either[HotelsServiceError, Unit]] = {
    val operation = for {
      _ <- db.editDescription(id, description)
    } yield ()

    operation.attempt.map {
      case Right(_) => Right(())
      case Left(_)  => Left(HotelsServiceError.InternalError.default)
    }
  }

  override def deleteHotel(
      id: Hotel.Id
  ): F[Either[HotelsServiceError, Unit]] = {
    val operation = for {
      _ <- db.delete(id)
    } yield ()

    operation.attempt.map {
      case Right(_) => Right(())
      case Left(_)  => Left(HotelsServiceError.InternalError.default)
    }
  }

}

object HotelsServiceImpl {
  def impl[F[_]: MonadThrow](
      dao: HotelDao[F],
      kafkaProducer: KafkaProducer[F, String, String],
      topic: String
  ): HotelsService[F] = new HotelsServiceImpl[F](dao, kafkaProducer, topic)
}
