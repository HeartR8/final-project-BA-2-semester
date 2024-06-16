package tours.config

case class KafkaConfig(
    bootstrapServers: String,
    toursTopic: String,
    hotelsTopic: String,
    ticketsTopic: String,
    consumerGroupId: String
)
