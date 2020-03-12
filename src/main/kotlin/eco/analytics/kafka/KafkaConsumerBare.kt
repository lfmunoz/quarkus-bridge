package eco.analytics.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import eco.analytics.mapper
import eco.analytics.rabbit.RabbitConsumerBare
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.serialization.StringDeserializer
import org.fissore.slf4j.FluentLoggerFactory
import java.time.Duration
import java.util.*


class KafkaConsumerBare<T>(
        private val clazz: Class<T>
) {


    companion object {
        private val log = FluentLoggerFactory.getLogger(KafkaConsumerBare::class.java)
    }


    //    private val bootstrapServer: String = "oc112-22.maas.auslab.2wire.com:9092",
    lateinit var groupId : String
    lateinit var topic : String
    lateinit var bootstrapServer : String

    // NOT thread-safe -  All network I/O happens in the thread of the application making the call.
    //  More consumers means more TCP connections to the cluster (one per thread).
    private lateinit var aKafkaConsumer: KafkaConsumer<String, String>

    @InternalCoroutinesApi
    fun connect(config_bootstrapServer: String, config_groupId: String, config_topic: String) : Flow<T> {
        bootstrapServer = config_bootstrapServer
        groupId = config_groupId
        topic = config_topic
        aKafkaConsumer = KafkaConsumer<String, String>(producerProps(bootstrapServer, groupId ))
        aKafkaConsumer.subscribe(listOf(topic))
        return consumerFlow()
    }

    @InternalCoroutinesApi
    private fun consumerFlow() : Flow<T> {
        return flow {
            try {
                while (true) {
                    val record = aKafkaConsumer.poll(Duration.ofMillis(100))
                    record.forEach {
                        val obj = mapper.readValue(it.value(), clazz )
                        log.info().log("[consume] - ${obj.toString()}")
                        emit(obj)
                    }
                }
            } catch(e: WakeupException) {
                println("received shutdown signal")
            } catch(e: Exception) {
                e.printStackTrace()
            } finally {
                aKafkaConsumer.close()
            }
        }
    }


    private fun producerProps(bootstrapServer: String, groupdId: String): Properties {
        val deserializer = StringDeserializer::class.java.canonicalName
        val props = Properties()
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer)
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupdId)
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, deserializer)
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializer)
        props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        return props
    }

    fun close() {
        aKafkaConsumer.wakeup()
    }


}