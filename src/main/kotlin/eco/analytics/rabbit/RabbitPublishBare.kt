package eco.analytics.rabbit

import com.rabbitmq.client.*
import io.vertx.core.json.JsonObject
import io.vertx.rabbitmq.RabbitMQOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import org.fissore.slf4j.FluentLoggerFactory
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.concurrent.NotThreadSafe


/**
 *  https://www.rabbitmq.com/java-client.html
 *  https://www.rabbitmq.com/api-guide.html
 *
 *  Publisher Confirms:
 *      https://www.rabbitmq.com/tutorials/tutorial-seven-java.html
 */
class RabbitPublisherBare(
    private val config_uri: String
) {
    private val log = FluentLoggerFactory.getLogger(RabbitPublisherBare::class.java)

    lateinit var exchangeName: String
    lateinit var uri: String

    val connected : AtomicBoolean = AtomicBoolean(false)

    private val factory: ConnectionFactory = ConnectionFactory()
    private val connection: Connection
    private var aChannel: Channel? = null   // Channel is NOT thread safe
    // ________________________________________________________________________________
    // CONSTRUCTOR
    // ________________________________________________________________________________
    init {
        factory.setUri(config_uri)
        connection = factory.newConnection()
    }

    // ________________________________________________________________________________
    // PUBLIC
    // ________________________________________________________________________________
    suspend fun connect(config_exchange: String) : Boolean {
        log.info().log("connecting to $config_uri $config_exchange ")
        exchangeName = config_exchange
        uri = config_uri
        aChannel = connection.createChannel()
        createExchange(aChannel, exchangeName)
        return true
    } // end of connect

    fun publish(aByteArray: ByteArray) {
        aChannel?.basicPublish(exchangeName, "", byteArrayRabbitMessage, aByteArray) ?: throw RuntimeException("[basicPublish] - channel is null")
        log.debug().log("[basicPublish] - sent bytearray of length ${aByteArray.size}")
    }
    fun publish(json: JsonObject) {
        aChannel?.basicPublish(exchangeName, "",  MessageProperties.TEXT_PLAIN , json.toBuffer().bytes) ?: throw RuntimeException("[basicPublish] - channel is null")
        log.debug().log("[basicPublish] - sent $json")
    }

    private val byteArrayRabbitMessage : AMQP.BasicProperties =
        AMQP.BasicProperties.Builder()
                .contentType("application/octet-stream")
                .deliveryMode(1) // nonpersistent
                .priority(1)
                .appId("eco-analytics-bridge")
                .build()

    private fun buildTextRabbitMessage() : AMQP.BasicProperties{
        return AMQP.BasicProperties.Builder()
                .contentType("text/plain")
                .deliveryMode(1)
                .priority(1)
                .appId("eco-analytics-bridge")
                .build()
    }


    fun close() {
        connection.close();
    }

    // ________________________________________________________________________________
    // PRIVATE
    // ________________________________________________________________________________
    private fun createExchange(aChannel: Channel?,  exchangeName: String) {
        aChannel?.exchangeDeclare(exchangeName, "fanout", false) ?: throw RuntimeException("[exchangeDeclare] - channel is null")
        log.info().log("[exchange created] - exchange=$exchangeName")
    }

}

