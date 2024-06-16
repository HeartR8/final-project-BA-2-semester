package tours.models

import io.circe.generic.JsonCodec

@JsonCodec
case class TicketEvent(hotelId: String, event: String)
