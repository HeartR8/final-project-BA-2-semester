package tours.models

import io.circe.generic.JsonCodec

@JsonCodec
case class HotelEvent(hotelId: String, event: String)
