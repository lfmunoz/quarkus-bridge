package eco.analytics.monitor

import com.fasterxml.jackson.databind.ObjectMapper
import eco.analytics.GenericResult
import eco.analytics.kafka.KafkaConfig
import eco.analytics.kafka.KafkaMessage
import eco.analytics.kafka.KafkaPublisherBare
import eco.analytics.mapper
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import org.fissore.slf4j.FluentLoggerFactory
import javax.annotation.PostConstruct
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
open class KeyValueConfigurator(
//      private val aKafkaPublisher: KafkaPublisherBare
) {

    private val log = FluentLoggerFactory.getLogger(KeyValueConfigurator::class.java)

    // TODO: value should be some sort of container object with value, type, and other info
    val keyValueMap: MutableMap<String, String> = mutableMapOf(
            "key1" to "1",
            "key2" to "2"
    )

//    val aKafkaPublisher = KafkaPublisherBare(KafkaConfig(
//           bootstrapServer = "10.37.240.46:9094",
//            topic = "test-config"
//    ))

    @POST
    @Path("/set")
    fun set(aKeyValueDto: KeyValueDto): JsonObject {
        log.debug().log("[set] - key=$aKeyValueDto")
        keyValueMap[aKeyValueDto.key]?.let {
            (it.toInt() + 1).toString()
        } ?: run {
            keyValueMap[aKeyValueDto.key] = "1"
        }
        return JSON_OK
    }

    @GET
    @Path("/view")
    fun inspect(): String {
        log.debug().log("[inspect]")
        return mapper.writeValueAsString(keyValueMap)
    }

    @GET
    @Path("/send")
    fun sendToKafka(): JsonObject {
        log.debug().log("[sendToKafka]")
        val aKafkaMessage = KafkaMessage(
                key = "api-send".toByteArray(),
                value  = mapper.writeValueAsBytes(keyValueMap)
        )

        
//        return when(val result = aKafkaPublisher.publishMessage(aKafkaMessage)) {
//            is GenericResult.Success -> JSON_OK
//            is GenericResult.Failure -> JSON_NOK
//        }
        return JSON_OK
    }


}

val OK = "OK"
val NOK = "NOK"

val JSON_OK = json{obj("return" to OK)}
val JSON_NOK = json{obj("return" to NOK)}


data class KeyValueDto (
    val key: String,
    val type: ValueType,
    val value: String
)

enum class ValueType(value: String) {
   INTEGER("INTEGER"),
   STRING("STRING"),
   DOUBLE("DOUBLE")
}