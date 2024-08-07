package hotels.models

import io.circe.generic.JsonCodec

import java.util.UUID

@JsonCodec
case class OutboxEvent(eventId: UUID, hotelId: String, event: String, processed: Boolean)
