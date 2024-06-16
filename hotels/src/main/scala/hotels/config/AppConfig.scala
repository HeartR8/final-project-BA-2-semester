package hotels.config

case class AppConfig(
                      http: HttpServer,
                      database: PostgresConfig,
                      kafka: KafkaConfig
                    )

