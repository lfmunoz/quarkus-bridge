package eco.analytics

import eco.analytics.kafka.KafkaPublisherBare
import eco.analytics.rabbit.ElementDataModel
import eco.analytics.rabbit.RabbitConsumerBare
import eco.model.ElementData
import eco.model.ElementDataList
import io.vertx.axle.core.eventbus.EventBus
import io.vertx.axle.core.eventbus.Message
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


@Path("/api")
open class WebMain {

    private val log = FluentLoggerFactory.getLogger(WebMain::class.java)

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

    init {
        scope.launch {

        }
    }


    @InternalCoroutinesApi
    suspend fun start() {
        val aRabbitConsumerBare = RabbitConsumerBare(uri)
        val aKafkaPublisherBare = KafkaPublisherBare(ElementData::class.java)

        // start consuming rabbit messages
        val elementDataFlow = aRabbitConsumerBare.connect(elementDataExchange, elementDataQueue)
            .map {
                val aElementDataList = mapper.readValue(it, ElementDataList::class.java)
                aElementDataList.values()
            }.flatMapConcat {
                // val bytes = mapper.writeValueAsBytes(it)
                it.asFlow()
            } //.produceIn(myScope)
        aKafkaPublisherBare.publish(elementDataFlow)
    }

    fun stop() {
//        aKafkaConsumerBare.close()
    }

}


class KafkaMessage(
       val key: String,
       val value: String
)
