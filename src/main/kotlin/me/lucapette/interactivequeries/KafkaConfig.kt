package me.lucapette.interactivequeries

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("kafka")
data class KafkaConfig(
    val bootstrapServers: String,
    val instanceId: String,
    val rpcHost: String,
    val rpcPort: Int,
)
