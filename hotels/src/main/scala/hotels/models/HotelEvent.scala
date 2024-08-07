package hotels.models

import io.circe.generic.JsonCodec

@JsonCodec
case class HotelEvent(event: String, hotel: Option[Hotel] = None)
