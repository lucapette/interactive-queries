package me.lucapette.interactivequeries

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class IngestionController(private val producer: KafkaProducer<String, String>) {

    @PostMapping("/accept")
    fun accept(@RequestBody() input: IngestionRequest) {
        producer.send(ProducerRecord("words", input.word.lowercase(), input.word)).get()
    }
}

data class IngestionRequest(val word: String)