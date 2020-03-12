package eco.analytics.rabbit

import eco.analytics.GenericResult
import eco.model.ElementData
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.Json
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.toChannel
import io.vertx.kotlin.rabbitmq.*
import io.vertx.rabbitmq.RabbitMQClient
import io.vertx.rabbitmq.RabbitMQOptions
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.fissore.slf4j.FluentLoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.enterprise.context.ApplicationScoped


/**
 * Service - OurRabbitListener
 *   Listen for RPC requests and process them
 */
//@ApplicationScoped
class RabbitConsumerVertx // : CoroutineVerticle() {
{
    private val log = FluentLoggerFactory.getLogger(RabbitConsumerVertx::class.java)

    val vertx: Vertx = Vertx.vertx()
    var aRabbitMQClient: RabbitMQClient? = null
//    var aReceiveChannel: ReceiveChannel<List<ElementData>>? = null

    lateinit var consumerQueueName: String
    lateinit var exchangeName: String
    lateinit var uri: String

    val connected : AtomicBoolean = AtomicBoolean(false)

    // ________________________________________________________________________________
    // CONSTRUCTOR
    // ________________________________________________________________________________
//    init { }

    // ________________________________________________________________________________
    // PUBLIC
    // ________________________________________________________________________________
    suspend fun connect(config_uri: String, config_exchange: String, config_queue: String) : ReceiveChannel<Buffer> {


        log.info().log("connecting to $config_uri $config_exchange $config_queue")
        consumerQueueName = config_queue
        exchangeName = config_exchange
        uri = config_uri

        val bRabbitMQClient = RabbitMQClient.create(vertx, buildRabbitMQOptions(uri))
        aRabbitMQClient = bRabbitMQClient
        bRabbitMQClient.startAwait()

        var count = 0
        createQueueAndBindToExchange(bRabbitMQClient, consumerQueueName, exchangeName)
        val aRabbitMQConsumer = bRabbitMQClient.basicConsumerAwait(consumerQueueName)
//        aRabbitMQConsumer.handler {
//            log.info().log("received message ! ${count++}")
//            resultList.add(it.body())
//        }
        return aRabbitMQConsumer.toChannel(vertx).map{
                        log.info().log("received message ! ${count++}")
            it.body()
        }
//            return flow {
//                val aElementDataList = Json.decodeValue(it.body(), ElementDataList::class.java)
//                emit(aRabbitMQMessage.body())

//                log.info().log("creating flow ...")
//                resultList.forEach {
//                    log.info().log("admit flow ...")
//                    emit(it)
//                }
//                aElementDataList.values().forEach { emit(it) }
//            }

//            val aRabbitMQConsumer = bRabbitMQClient.basicConsumerAwait(consumerQueueName)
//            log.info().log("RabbitMQ consumer created !")
//            for(aRabbitMQMessage in  aRabbitMQConsumer.toChannel(vertx)) {

//                log.info().log("received message ! ${count++}")
//                val aElementDataList = Json.decodeValue(aRabbitMQMessage.body(), ElementDataList::class.java)
//                aElementDataList.values().forEach { emit(it) }
//                emit(aRabbitMQMessage.body())
//            }
    } // end of connect

    suspend fun close() {
        aRabbitMQClient?.stopAwait()
    }

    // ________________________________________________________________________________
    // PRIVATE
    // ________________________________________________________________________________
    fun buildRabbitMQOptions(uri: String): RabbitMQOptions {
        var aRabbitMQOptions = RabbitMQOptions()
        aRabbitMQOptions.uri = uri // "amqp://userName:password@hostName:portNumber/virtualHost"
        aRabbitMQOptions.connectionTimeout = 6000
        aRabbitMQOptions.requestedHeartbeat = 60
        aRabbitMQOptions.handshakeTimeout = 6000
        aRabbitMQOptions.requestedChannelMax = 0
        aRabbitMQOptions.networkRecoveryInterval = 500
        aRabbitMQOptions.isAutomaticRecoveryEnabled = true
        return aRabbitMQOptions
    }


    suspend fun createExchange(exchangeName: String) {
        var config = json {
            obj( )
        }
//        config.put("x-dead-letter-exchange", "my.deadletter.exchange")
//        config.put("alternate-exchange", "my.alternate.exchange")
        try {
            aRabbitMQClient?.exchangeDeclareAwait(exchangeName, "fanout", false, true, config)
        } catch(e: Throwable) {
            e.printStackTrace()
        }
    }


    suspend fun createQueueAndBindToExchange(aRabbitMQClient: RabbitMQClient, queueName: String, exchangeName: String) {
        createExchange(exchangeName)
        val aJsonObjectConfig = json {
            obj("x-message-ttl" to 10000L)
        }
        val aJsonObjectResult = aRabbitMQClient.queueDeclareAwait(
                queue = queueName,
                durable = true,
                exclusive = false,
                autoDelete = true,
                config = aJsonObjectConfig)
        log.info().log("[queue created] - $aJsonObjectResult")
        aRabbitMQClient.queueBindAwait(queueName, exchangeName, "")
    }


} // EOF


