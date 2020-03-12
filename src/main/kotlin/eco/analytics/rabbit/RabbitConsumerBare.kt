package eco.analytics.rabbit

import com.rabbitmq.client.*
import com.rabbitmq.client.impl.MicrometerMetricsCollector
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.fissore.slf4j.FluentLoggerFactory
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean


/**
 *  https://www.rabbitmq.com/java-client.html
 *  https://www.rabbitmq.com/api-guide.html
 */
class RabbitConsumerBare(
    private val config_uri: String
) {

    companion object {
        private val log = FluentLoggerFactory.getLogger(RabbitConsumerBare::class.java)
        const val EXCHANGE_TYPE: String = "fanout"
        const val EXCHANGE_DURABLE: Boolean  = true
    }


    lateinit var queueName: String
    lateinit var exchangeName: String
    lateinit var uri: String

    val connected : AtomicBoolean = AtomicBoolean(false)

    private val factory: ConnectionFactory = ConnectionFactory()
    private var connection: Connection? = null

    // ________________________________________________________________________________
    // CONSTRUCTOR
    // ________________________________________________________________________________
    init {
        factory.setUri(config_uri)
        factory.isAutomaticRecoveryEnabled = true // // attempt recovery every 5 seconds
//        val metrics = MicrometerMetricsCollector()
//        connectionFactory.setMetricsCollector(metrics)
    }

    // ________________________________________________________________________________
    // PUBLIC
    // ________________________________________________________________________________
    suspend fun connect(config_exchange: String, config_queue: String) : Flow<ByteArray>{
        try {
            connection = factory.newConnection()
        } catch(e: Throwable) {
            throw RuntimeException("Can't connect")
        }

        log.info().log("connecting to $config_uri $config_exchange $config_queue")
        queueName = config_queue
        exchangeName = config_exchange
        uri = config_uri
        // Channel is NOT thread safe
        val aChannel: Channel = connection?.createChannel() ?: throw RuntimeException("Connection is null")
        createQueueAndBindToExchange(aChannel, queueName, exchangeName)
        val autoAck = false
        return aChannel.basicConsumeFlow(queueName, autoAck, "myConsumerTag")
    } // end of connect



    fun close() {
//        channel.close();
        connection?.close();
    }

    // ________________________________________________________________________________
    // PRIVATE
    // ________________________________________________________________________________
    private fun createQueueAndBindToExchange(aChannel: Channel, queueName: String, exchangeName: String) {
        aChannel.exchangeDeclare(exchangeName, EXCHANGE_TYPE, EXCHANGE_DURABLE)
        aChannel.queueDeclare(queueName, false, false, false, null)
        aChannel.queueBind(queueName, exchangeName, "")
        log.info().log("[queue created] - queue=$queueName  exchange=$exchangeName")
    }

    fun Channel.basicConsumeFlow(queue: String, autoAck: Boolean,  consumerTag:String ) : Flow<ByteArray> =
            callbackFlow {
                basicConsume(queue,autoAck, consumerTag, object : DefaultConsumer(this@basicConsumeFlow) {

                @Throws(IOException::class)
                override fun handleDelivery(
                        consumerTag: String?,
                        envelope: Envelope,
                        properties: AMQP.BasicProperties,
                        body: ByteArray?
                ) {
                    val routingKey: String = envelope.routingKey
                    val contentType = properties.contentType
                    val deliveryTag: Long = envelope.deliveryTag
                    log.warn().log("[basicConsume] - bytearray")
                    body?.let {
                        offer(it)
                    } ?: log.warn().log("Got null for ByteArray ")
                    channel.basicAck(deliveryTag, false)
                }
            })
//                handleShutdownSignal is called when channels and connections close
//                handleConsumeOk is passed the consumer tag before any other callbacks to that Consumer are called.
//                Consumers can also implement the handleCancelOk and handleCancel methods to be notified of explicit and implicit cancellations, respectively.
//                channel.basicCancel(consumerTag);  explicitly cancel a particular consumer
                awaitClose { this@basicConsumeFlow.close() }
            }



}
