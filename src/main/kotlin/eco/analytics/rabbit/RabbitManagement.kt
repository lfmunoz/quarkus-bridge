package eco.analytics.rabbit

import eco.analytics.ELEMENT_DATA_ADDR
import eco.analytics.mapper
import eco.model.ElementDataList
import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import io.quarkus.vertx.ConsumeEvent
import io.vertx.axle.core.eventbus.EventBus
import io.vertx.core.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import org.fissore.slf4j.FluentLoggerFactory
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.event.Observes
import javax.inject.Inject

@ApplicationScoped
open class RabbitManagement {

    companion object {
        private val log = FluentLoggerFactory.getLogger(RabbitManagement::class.java)

    }

    @Inject lateinit var bus: EventBus

//    @Inject lateinit var mapper: ObjectMapper


    private val exchangeName = "eco-analytics-test-publish"
    private val queueName = "eco-analytics-test-queue"
    private val uri = "amqp://guest:guest@10.37.240.51:5672"

    private val elementDataExchange = "element-data"
    private val elementDataQueue = "elementData.queue"

    val job = Job()
    val context = newSingleThreadContext("rabbit-mgr")
    val scope = CoroutineScope(context + job)

    suspend fun createConsumer() {
        val aRabbitConsumerVertx = RabbitConsumerBare(uri)
        val flow = aRabbitConsumerVertx.connect(elementDataExchange, elementDataQueue)
        flow.collect {
            val aElementDataList = mapper.readValue(it, ElementDataList::class.java)
             aElementDataList.values().forEach {
                 val bytes = mapper.writeValueAsBytes(it)
                 bus.publish(ELEMENT_DATA_ADDR, bytes)
             }
        }
    }

    fun onStart(@Observes ev: StartupEvent?) {
        log.info().log("---------------------------------------------------------------")
        log.info().log("The application is starting...")
        log.info().log("---------------------------------------------------------------")

        scope.launch {
            createConsumer()
        }
    }

    fun onStop(@Observes ev: ShutdownEvent?) {
        log.info().log("---------------------------------------------------------------")
        log.info().log("The application is stopping...")
        log.info().log("---------------------------------------------------------------")
    }


//    @ConsumeEvent(value = "blocking-consumer", blocking = true)
    @ConsumeEvent("blocking-consumer")
    fun consume(name: String): String {
        log.info().log("got message $name")
        return name.toUpperCase()
    }

}