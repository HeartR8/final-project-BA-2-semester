package tours.models

import enumeratum.EnumEntry.Lowercase
import enumeratum._
sealed trait KafkaEvents extends EnumEntry with Lowercase

object KafkaEvents extends Enum[KafkaEvents] {
  val values = findValues

  case object Add extends KafkaEvents

  case object Edit extends KafkaEvents

  case object Delete extends KafkaEvents
}
