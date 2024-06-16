package hotels.config

case class KafkaConfig(
    bootstrapServers: String,
    topic: String
)
