package tickets.models

import enumeratum.{Enum, EnumEntry}
import enumeratum.EnumEntry.Lowercase

sealed trait KafkaEvents extends EnumEntry with Lowercase

object KafkaEvents extends Enum[KafkaEvents] {
  val values = findValues

  case object Add extends KafkaEvents

  case object Edit extends KafkaEvents

  case object Delete extends KafkaEvents
}
