package me.lucapette.interactivequeries

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.web.client.RestTemplate
import java.time.Duration


@SpringBootApplication
@EnableConfigurationProperties(KafkaConfig::class)
class InteractiveQueriesApplication {
    @Bean
    fun producer(config: KafkaConfig): KafkaProducer<String, String> {
        return KafkaProducer(
            mapOf(
                "bootstrap.servers" to config.bootstrapServers,
                "key.serializer" to StringSerializer::class.java.name,
                "value.serializer" to StringSerializer::class.java.name,
            )
        )
    }

    @Bean
    fun restTemplate(): RestTemplate = RestTemplateBuilder()
        .setConnectTimeout(Duration.ofSeconds(10))
        .build()
}

fun main(args: Array<String>) {
    runApplication<InteractiveQueriesApplication>(*args)
}
