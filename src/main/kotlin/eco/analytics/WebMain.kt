package eco.analytics

import eco.analytics.kafka.KafkaMessage
import eco.analytics.kafka.KafkaPublisherBare
import eco.analytics.rabbit.ElementDataModel
import eco.analytics.rabbit.RabbitConsumerBare
import eco.model.ElementData
import eco.model.ElementDataList
import io.vertx.axle.core.eventbus.EventBus
import io.vertx.axle.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams
import org.fissore.slf4j.FluentLoggerFactory
import org.reactivestreams.Publisher
import java.util.*
import java.util.concurrent.CompletionStage
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType


@InternalCoroutinesApi
@Path("/api")
open class WebMain {

    companion object {
        private val log = FluentLoggerFactory.getLogger(WebMain::class.java)
    }

    // RABBIT_MQ
    private val uri = "amqp://guest:guest@10.37.240.51:5672"
    private val elementDataExchange = "element-data"
    private val elementDataQueue= "element-data-queue"
    private val parameterMessageExchange = "parameter-message"
    private val monitorMessageExchange = "monitor-message"

    // KAFKA
    private val bootstrapServer = "10.37.240.46:9094"
    private val groupId: String = "element"
    private val topic: String = "element-ts"

    val job = Job()
    val context = newSingleThreadContext("rabbit-mgr")
    val scope = CoroutineScope(context + job)

    @Inject
    lateinit var bus: EventBus


    @GET
    @Path("bridge-start")
    @Produces(MediaType.TEXT_PLAIN)
//    @Produces(MediaType.APPLICATION_JSON)
    fun bridgeStart(): String {
        log.info().log("[api call to bridge-start]")
        scope.launch {
            try {
                start()
            } catch (e: Exception) {
                log.error().withCause(e).log("Error in bridge between rabbit and kafka")
            }
        }
        return "OK"
//        return json {
//            obj("running" to true)
//        }
    }



    init {

    }


    @InternalCoroutinesApi
    suspend fun start() {
        log.info().log("Starting bridge between RABBIT and KAFKA")
        val aRabbitConsumerBare = RabbitConsumerBare(uri)
        val aKafkaPublisherBare = KafkaPublisherBare()
        try {
            aKafkaPublisherBare.connect(bootstrapServer, topic)
            // start consuming rabbit messages
            val elementDataFlow = aRabbitConsumerBare.connect(elementDataExchange, elementDataQueue)
                    .map {
                        val aElementDataList = mapper.readValue(it, ElementDataList::class.java)
                        aElementDataList.values()
                    }.flatMapConcat {
                        // val bytes = mapper.writeValueAsBytes(it)
                        it.asFlow()
                    }.map {
                        val key = it.id()
//                        val value = mapper.writeValueAsString(it.properties())
                        val value = mapper.writeValueAsString(it)
                        KafkaMessage(key, value)
                    }
            //.produceIn(myScope)
            val result = aKafkaPublisherBare.publishFlow(elementDataFlow)
            log.info().log("$result")
        } finally {
            aKafkaPublisherBare.close()
            aRabbitConsumerBare.close()
        }
    }


}


