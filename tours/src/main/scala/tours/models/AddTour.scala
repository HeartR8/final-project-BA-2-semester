package tours.models

import io.circe.generic.JsonCodec

@JsonCodec
final case class AddTour(
    hotelId: Hotel.Id,
    ticketToId: Ticket.Id,
    ticketFromId: Ticket.Id
)
