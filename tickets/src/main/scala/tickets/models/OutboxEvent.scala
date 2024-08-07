package tickets.models

import io.circe.generic.JsonCodec

import java.util.UUID

@JsonCodec
case class OutboxEvent(eventId: UUID, ticketId: String, event: String, processed: Boolean)
