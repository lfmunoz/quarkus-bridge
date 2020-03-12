package eco.analytics.rabbit

import eco.model.*
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.toChannel
import io.vertx.kotlin.rabbitmq.*
import io.vertx.rabbitmq.RabbitMQClient
import io.vertx.rabbitmq.RabbitMQOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.fissore.slf4j.FluentLoggerFactory
import javax.enterprise.context.ApplicationScoped


/**
 * Service - OurRabbitListener
 *   Listen for RPC requests and process them
 */
//@ApplicationScoped
class RabbitPublisherVertx // : CoroutineVerticle() {
{

    private val log = FluentLoggerFactory.getLogger(RabbitPublisherVertx::class.java)

//    http://localhost:15672/api/exchanges
//    http://localhost:15672/api/queues
//    http://localhost:15672/api/

    val vertx: Vertx = Vertx.vertx()
    var aRabbitMQClient: RabbitMQClient? = null
    lateinit var exchangeName: String
    lateinit var uri: String

    // ________________________________________________________________________________
    // CONSTRUCTOR
    // ________________________________________________________________________________
//    init { }

    // ________________________________________________________________________________
    // PUBLIC
    // ________________________________________________________________________________
    suspend fun connect(config_uri: String, config_exchangeName: String): String {
        log.info().log("connecting to $config_uri $config_exchangeName")
        uri = config_uri
        exchangeName = config_exchangeName
        val bRabbitMQClient = RabbitMQClient.create(vertx, buildRabbitMQOptions(uri))
        aRabbitMQClient = bRabbitMQClient
        bRabbitMQClient.startAwait()
        createExchange(exchangeName)
        // Put the channel in confirm mode. This can be done once at init.
        bRabbitMQClient.confirmSelectAwait()
        return "OK"
    } // end of connect

    suspend fun publishWithConfirm(message: JsonObject) {
        log.trace().log("publishWithConfirm to $message ")
       aRabbitMQClient?.basicPublishAwait(exchangeName, "", message)
       aRabbitMQClient?.waitForConfirmsAwait()
    }

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
        aRabbitMQOptions.requestedChannelMax = 5
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
            aRabbitMQClient?.exchangeDeclareAwait(exchangeName, "fanout", false, false, config)
        } catch(e: Throwable) {
            e.printStackTrace()
        }
    }

} // EOF

