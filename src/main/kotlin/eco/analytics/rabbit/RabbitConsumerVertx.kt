package eco.analytics.rabbit

import eco.model.*
import eco.model.amqp.converter.WireMessageConverter
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.buffer.impl.BufferImpl
import io.vertx.core.json.Json
import io.vertx.core.json.Json.mapper
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.coroutines.toChannel
import io.vertx.kotlin.rabbitmq.*
import io.vertx.rabbitmq.RabbitMQClient
import io.vertx.rabbitmq.RabbitMQMessage
import io.vertx.rabbitmq.RabbitMQOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.fissore.slf4j.FluentLoggerFactory
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.support.converter.MessageConversionException
import org.springframework.util.StringUtils
import javax.enterprise.context.ApplicationScoped
import javax.inject.Singleton


/**
 * Service - OurRabbitListener
 *   Listen for RPC requests and process them
 */
@ApplicationScoped
class RabbitConsumerVertx // : CoroutineVerticle() {
{

    private val log = FluentLoggerFactory.getLogger(RabbitConsumerVertx::class.java)

//    http://localhost:15672/api/exchanges
//    http://localhost:15672/api/queues
//    http://localhost:15672/api/


    val vertx: Vertx = Vertx.vertx()
//    val job = Job()
//    val scope = CoroutineScope(aVertx.dispatcher() + job)

    val consumerQueueName = "eco.analytics"
    val exchangeName = "element-data"
//    var aReceiveChannel: ReceiveChannel<List<ElementData>>? = null
    var aRabbitMQClient: RabbitMQClient? = null

    lateinit var aReceiveChannel: ReceiveChannel<List<ElementData>>
    // ________________________________________________________________________________
    // CONSTRUCTOR
    // ________________________________________________________________________________
    suspend fun start() {
//        val uri = "amqp://guest:guest@10.37.240.51:5672"
//        connect(uri)
    }

    suspend fun stop() {
        aRabbitMQClient?.stopAwait()
    }

    init {
        runBlocking {
            val uri = "amqp://guest:guest@10.37.240.51:5672"
            aReceiveChannel = connect(uri)
//            aVertx.deployVerticleAwait(aRabbitConsumerVertx)
        }
    }

    // ________________________________________________________________________________
    // PUBLIC
    // ________________________________________________________________________________
    suspend fun connect(uri: String) : ReceiveChannel<List<ElementData>> {
        val bRabbitMQClient = RabbitMQClient.create(vertx, buildRabbitMQOptions(uri))
        aRabbitMQClient = bRabbitMQClient
        bRabbitMQClient.startAwait()

        createQueueAndBindToExchange(bRabbitMQClient, consumerQueueName, exchangeName)
        val aRabbitMQConsumer = bRabbitMQClient.basicConsumerAwait(consumerQueueName)
        log.info().log("RabbitMQ consumer created !")
        return aRabbitMQConsumer.toChannel(vertx).map {
            log.info().log("Received ElementDataList!")
            Json.decodeValue(it.body(), ElementDataList::class.java)
        }.map { it.values() }

    } // end of withContext


    // ________________________________________________________________________________
    // Helper methods
    // ________________________________________________________________________________
    fun buildRabbitMQOptions(uri: String): RabbitMQOptions {
        var aRabbitMQOptions = RabbitMQOptions()
        aRabbitMQOptions.uri = uri // "amqp://userName:password@hostName:portNumber/virtualHost"
        aRabbitMQOptions.connectionTimeout = 6000
        aRabbitMQOptions.requestedHeartbeat = 60
        aRabbitMQOptions.handshakeTimeout = 6000
        aRabbitMQOptions.requestedChannelMax = 5
        aRabbitMQOptions.networkRecoveryInterval = 500
        aRabbitMQOptions.isAutomaticRecoveryEnabled = true
        return aRabbitMQOptions
    }

    suspend fun createQueueAndBindToExchange(aRabbitMQClient: RabbitMQClient, queueName: String, exchangeName: String) {
        val aJsonObjectConfig = json {
            obj("x-message-ttl" to 10000L)
        }
        val aJsonObjectResult = aRabbitMQClient.queueDeclareAwait(
                queue = queueName,
                durable = false,
                exclusive = false,
                autoDelete = true,
                config = aJsonObjectConfig)
        log.info().log("[queue created] - $aJsonObjectResult")
        aRabbitMQClient.queueBindAwait(queueName, exchangeName, "")
    }

    fun close() {
        aRabbitMQClient?.stop {
            if(it.succeeded()) {
                log.info().log("[rabbit client closed] - succeeded ")
            } else {
                log.info().log("[rabbit client closed] - failed")
            }
        }
    }


} // EOF

