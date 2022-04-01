package me.lucapette.interactivequeries

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.*
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Grouped
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.ApplicationListener
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class WordsCountController : ApplicationListener<ApplicationStartedEvent> {
    private final val kafkaStreams: KafkaStreams

    companion object {
        val log: Logger = LoggerFactory.getLogger(WordsCountController::class.java)
    }

    init {
        val props = Properties()

        props[StreamsConfig.APPLICATION_ID_CONFIG] = "words-count-controller-v1"
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"
        props[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.String().javaClass.name
        props[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.String().javaClass.name

        val config = StreamsConfig(props)

        kafkaStreams = KafkaStreams(buildTopology(), config)
    }

    private fun buildTopology(): Topology {
        val builder = StreamsBuilder()

        builder.stream<String, String>("words", Consumed.`as`("words_input_topic"))
            .groupByKey(Grouped.`as`("group_by_word"))
            .count(Materialized.`as`("words_count"))

        val topology = builder.build()

        log.info(topology.describe().toString())

        return topology
    }

    override fun onApplicationEvent(event: ApplicationStartedEvent) {
        kafkaStreams.start()
    }

    @PostMapping("/search")
    fun search(@RequestBody input: SearchRequest): SearchResponse {
        val store = kafkaStreams.store(
            StoreQueryParameters.fromNameAndType(
                "words_count",
                QueryableStoreTypes.keyValueStore<String, Long>()
            )
        )

        return SearchResponse(input.query, store.get(input.query))
    }

}

data class SearchRequest(val query: String)

data class SearchResponse(val word: String, val count: Long)