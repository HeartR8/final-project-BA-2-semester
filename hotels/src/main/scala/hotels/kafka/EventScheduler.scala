package hotels.kafka

import cats.effect.Temporal
import cats.implicits._
import fs2.Stream
import fs2.kafka.{KafkaProducer, ProducerRecord, ProducerRecords}
import hotels.database.HotelDao
import org.typelevel.log4cats.SelfAwareStructuredLogger

import scala.concurrent.duration._

case class EventScheduler[F[_]: Temporal](
    dao: HotelDao[F],
    kafkaProducer: KafkaProducer[F, String, String],
    topic: String
)(implicit logger: SelfAwareStructuredLogger[F]) {
  def processOutboxEvents: F[Unit] = {
    logger.info("Starting outbox event processor") *>
      Stream.awakeEvery[F](3.seconds)
        .evalMap(_ => dao.fetchUnprocessedEvents)
        .evalMap { events =>
          events.traverse { event =>
            kafkaProducer.produce(ProducerRecords.one(ProducerRecord(
              topic,
              event.hotelId,
              event.event
            ))).flatten >> logger.info(
              s"Event sent ${event.hotelId} ${event.event}"
            ) >> dao.updateProcessedEvents(event.eventId)
          }
        }.compile.drain
  }
}

object EventScheduler {
  def impl[F[_]: Temporal](
      dao: HotelDao[F],
      kafkaProducer: KafkaProducer[F, String, String],
      topic: String
  )(implicit logger: SelfAwareStructuredLogger[F]): EventScheduler[F] =
    new EventScheduler[F](dao, kafkaProducer, topic)
}
