package me.lucapette.interactivequeries

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Grouped
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.state.HostInfo
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.ApplicationListener
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import java.util.Properties

@RestController
class WordsCountController(config: KafkaConfig, restTemplate: RestTemplate) :
    ApplicationListener<ApplicationStartedEvent> {
    private final val kafkaStreams: KafkaStreams
    private final val config: KafkaConfig
    private final val restTemplate: RestTemplate

    companion object {
        val log: Logger = LoggerFactory.getLogger(WordsCountController::class.java)
    }

    init {
        this.config = config
        this.restTemplate = restTemplate
        val props = Properties()

        props[StreamsConfig.APPLICATION_ID_CONFIG] = "words-count-controller-v1"
        props[StreamsConfig.APPLICATION_SERVER_CONFIG] = "#${config.rpcHost}:${this.config.rpcPort}"
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = config.bootstrapServers
        props[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.String().javaClass.name
        props[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.String().javaClass.name
        props[StreamsConfig.STATE_DIR_CONFIG] =
            "~/tmp/kafka-streams/words-count-controller-v1-${this.config.instanceId}"

        kafkaStreams = KafkaStreams(buildTopology(), StreamsConfig(props))
    }

    private fun buildTopology(): Topology {
        val builder = StreamsBuilder()

        builder.stream<String, String>("words", Consumed.`as`("words_input_topic"))
            .groupByKey(Grouped.`as`("group_by_word")).count(Materialized.`as`("words_count"))

        val topology = builder.build()

        log.info(topology.describe().toString())

        return topology
    }

    override fun onApplicationEvent(event: ApplicationStartedEvent) {
        kafkaStreams.start()
    }

    @PostMapping("/search")
    fun search(@RequestBody input: SearchRequest): ResponseEntity<SearchResponse> {
        val store = kafkaStreams.store(
            StoreQueryParameters.fromNameAndType(
                "words_count", QueryableStoreTypes.keyValueStore<String, Long>()
            )
        )

        val keyQueryMetadata =
            kafkaStreams.queryMetadataForKey("words_count", input.query, Serdes.String().serializer())

        return when (val activeHost = keyQueryMetadata.activeHost()) {
            HostInfo(config.rpcHost, config.rpcPort) -> {
                val count = store.get(input.query) ?: return ResponseEntity.notFound().build()
                ResponseEntity.ok(SearchResponse(input.query, count))
            }
            HostInfo.unavailable() -> ResponseEntity.notFound().build()
            else -> fetchViaRPC(activeHost, input)
        }
    }

    fun fetchViaRPC(activeHost: HostInfo, input: SearchRequest): ResponseEntity<SearchResponse> {
        return try {
            val response = restTemplate.postForEntity(
                "http://${activeHost.host()}:${activeHost.port()}/search", input, SearchResponse::class.java
            )
            if (response.statusCode.is2xxSuccessful) {
                ResponseEntity.ok(SearchResponse(input.query, response.body?.count ?: 0))
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: RestClientResponseException) {
            log.error("Error while trying to RPC from  ${activeHost.host()}:${activeHost.port()}", e)
            ResponseEntity.notFound().build()
        }
    }

}

data class SearchRequest(val query: String)

data class SearchResponse(val word: String, val count: Long)