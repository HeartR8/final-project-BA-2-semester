package tickets.models

import io.circe.generic.JsonCodec

@JsonCodec
case class TicketEvent(event: String, hotel: Option[Ticket] = None)
