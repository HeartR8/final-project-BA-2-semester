package tickets.kafka

import cats.effect.Temporal
import cats.implicits._
import fs2.Stream
import fs2.kafka.{KafkaProducer, ProducerRecord, ProducerRecords}
import io.circe.syntax.EncoderOps
import org.typelevel.log4cats.SelfAwareStructuredLogger
import tickets.database.TicketDao
import tickets.models.Ticket.Id
import tickets.models.{KafkaEvents, Ticket, TicketEvent}

import scala.concurrent.duration._

case class EventScheduler[F[_]: Temporal](
    dao: TicketDao[F],
    kafkaProducer: KafkaProducer[F, String, String],
    topic: String
)(implicit logger: SelfAwareStructuredLogger[F]) {

  private def matchEvent(ticketId: Ticket.Id, event: String) = {
    val kafkaEvent = KafkaEvents.withNameInsensitive(event)

    kafkaEvent match {
      case KafkaEvents.Add =>
        for {
          ticketEvent <- dao.get(ticketId).map(ticket => TicketEvent(event, ticket))
          _ <- kafkaProducer.produce(ProducerRecords.one(ProducerRecord(
            topic,
            ticketId.toString,
            ticketEvent.asJson.toString
          )))
        } yield ()
      case KafkaEvents.Edit =>
        for {
          ticketEvent <- dao.get(ticketId).map(ticket => TicketEvent(event, ticket))
          _ <- kafkaProducer.produce(ProducerRecords.one(ProducerRecord(
            topic,
            ticketId.toString,
            ticketEvent.asJson.toString
          )))
        } yield ()
      case KafkaEvents.Delete =>
        kafkaProducer.produce(ProducerRecords.one(ProducerRecord(
          topic,
          ticketId.toString,
          TicketEvent(event).asJson.toString
        ))).flatten
    }
  }

  def processOutboxEvents: F[Unit] = {
    logger.info("Starting outbox event processor") *>
      Stream.awakeEvery[F](3.seconds)
        .evalMap(_ => dao.fetchUnprocessedEvents)
        .evalMap { events =>
          {
            events.traverse { event =>
              {
                logger.info("Checking events") >> matchEvent(
                  Id.fromString(event.ticketId),
                  event.event
                ) >> logger.info(
                  s"Event sent ${event.ticketId} ${event.event}"
                ) >> dao.updateProcessedEvents(event.eventId)
              }
            }
          }
        }.compile.drain
  }
}

object EventScheduler {
  def impl[F[_]: Temporal](
      dao: TicketDao[F],
      kafkaProducer: KafkaProducer[F, String, String],
      topic: String
  )(implicit logger: SelfAwareStructuredLogger[F]): EventScheduler[F] = {
    new EventScheduler[F](dao, kafkaProducer, topic)
  }
}
