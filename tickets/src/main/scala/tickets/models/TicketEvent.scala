package tickets.models

import io.circe.generic.JsonCodec

@JsonCodec
case class TicketEvent(event: String, ticket: Option[Ticket] = None)
