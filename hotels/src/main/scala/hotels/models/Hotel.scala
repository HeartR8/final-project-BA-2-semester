package hotels.models

import doobie.Get
import io.circe.generic.JsonCodec
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.macros.newtype
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.{Codec, Schema}

import java.util.UUID

@JsonCodec
final case class Hotel(
                        id: Hotel.Id,
                        name: String,
                        description: Option[String] = None,
                        pricePerNight: Double
                      )

object Hotel {
  @newtype case class Id(value: UUID) {
    override def toString: String = value.toString
  }

  object Id {

    implicit val encoder: Encoder[Id] = Encoder[UUID].contramap(_.value)
    implicit val decoder: Decoder[Id] = Decoder[UUID].map(Id(_))
    implicit val codec: Codec[String, Id, TextPlain] = Codec.uuid.map(Id(_))(_.value)
    implicit val schema: Schema[Id] = deriving

    def fromString(s: String) = Id(UUID.fromString(s))

    implicit val idGet: Get[Id] = Get[String].map(Id.fromString)

    def random(): Id = Id(UUID.randomUUID())
  }
}
