package eco.analytics

import eco.analytics.rabbit.ElementDataModel
import io.vertx.axle.core.eventbus.EventBus
import io.vertx.axle.core.eventbus.Message

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


@Path("/rabbitmq")
open class WebRabbit {

    private val log = FluentLoggerFactory.getLogger(WebRabbit::class.java)


    private val exchangeName = "eco-analytics-test-publish"
    private val queueName = "eco-analytics-test-queue"
    private val uri = "amqp://guest:guest@10.37.240.51:5672"

    @Inject
    lateinit var bus: EventBus

    init {
//        val aRabbitPublisherBare = RabbitPublisherBare(uri)
//        aRabbitPublisherBare.connect(exchangeName)
//        repeat(messageCount) {
//            aRabbitPublisherBare.publish(genJsonObject())
//        }
    }

    @GET
    @Path("/elementDataModel")
    @Produces(MediaType.APPLICATION_JSON)
    fun elementDataModel(): CompletionStage<ElementDataModel> {
        return bus.request<ElementDataModel>(ELEMENT_DATA_MODEL_ADDR, System.currentTimeMillis()).thenApply {
            return@thenApply it.body()
        }
    }

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    fun stream(): Publisher<Double> {
        return ReactiveStreams.of(1.5, 4.0).buildRs()
    }


}