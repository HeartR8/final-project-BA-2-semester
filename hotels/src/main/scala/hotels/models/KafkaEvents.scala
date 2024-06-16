package hotels.models

sealed trait KafkaEvents

object KafkaEvents {
  case object Add extends KafkaEvents

  case object Edit extends KafkaEvents

  case object Delete extends KafkaEvents
}

