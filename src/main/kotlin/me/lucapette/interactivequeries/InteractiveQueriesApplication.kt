package me.lucapette.interactivequeries

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean


@SpringBootApplication
@EnableConfigurationProperties(KafkaConfig::class)
class InteractiveQueriesApplication {
    @Bean
    fun producer(): KafkaProducer<String, String> {
        return KafkaProducer(
            mapOf(
                "bootstrap.servers" to "localhost:9092",
                "key.serializer" to StringSerializer::class.java.name,
                "value.serializer" to StringSerializer::class.java.name,
            )
        )
    }
}

fun main(args: Array<String>) {
    runApplication<InteractiveQueriesApplication>(*args)
}
