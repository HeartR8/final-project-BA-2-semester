package hotels.kafka

import cats.effect.Temporal
import cats.implicits._
import fs2.Stream
import fs2.kafka.{KafkaProducer, ProducerRecord, ProducerRecords}
import hotels.database.HotelDao
import hotels.models.Hotel.Id
import hotels.models.{Hotel, HotelEvent, KafkaEvents}
import org.typelevel.log4cats.SelfAwareStructuredLogger

import scala.concurrent.duration._

case class EventScheduler[F[_] : Temporal](
                                            dao: HotelDao[F],
                                            kafkaProducer: KafkaProducer[F, String, String],
                                            topic: String
                                          )(implicit logger: SelfAwareStructuredLogger[F]) {

  private def matchEvent(hotelId: Hotel.Id, event: String) = {
    val kafkaEvent = KafkaEvents.withNameInsensitive(event)

    kafkaEvent match {
      case KafkaEvents.Add =>
        for {
          hotelEvent <- dao.get(hotelId).map(hotel => HotelEvent(event, hotel))
          _ <- kafkaProducer.produce(ProducerRecords.one(ProducerRecord(
            topic,
            hotelId.toString,
            hotelEvent.toString
          )))
        } yield ()
      case KafkaEvents.Edit =>
        for {
          hotelEvent <- dao.get(hotelId).map(hotel => HotelEvent(event, hotel))
          _ <- kafkaProducer.produce(ProducerRecords.one(ProducerRecord(
            topic,
            hotelId.toString,
            hotelEvent.toString
          )))
        } yield ()
      case KafkaEvents.Delete =>
        kafkaProducer.produce(ProducerRecords.one(ProducerRecord(
          topic,
          hotelId.toString,
          HotelEvent(event).toString
        ))).flatten
    }
  }

  def processOutboxEvents: F[Unit] = {
    logger.info("Starting outbox event processor") *>
      Stream.awakeEvery[F](3.seconds)
        .evalMap(_ => dao.fetchUnprocessedEvents)
        .evalMap { events =>
          events.traverse { event =>
            matchEvent(Id.fromString(event.hotelId), event.event) >> logger.info(
              s"Event sent ${event.hotelId} ${event.event}"
            ) >> dao.updateProcessedEvents(event.eventId)
          }
        }.compile.drain
  }
}

object EventScheduler {
  def impl[F[_] : Temporal](
                             dao: HotelDao[F],
                             kafkaProducer: KafkaProducer[F, String, String],
                             topic: String
                           )(implicit logger: SelfAwareStructuredLogger[F]): EventScheduler[F] =
    new EventScheduler[F](dao, kafkaProducer, topic)
}
