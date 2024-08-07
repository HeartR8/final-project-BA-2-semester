package tickets.config

case class KafkaConfig(
    bootstrapServers: String,
    topic: String
)
