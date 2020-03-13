package eco.analytics.kafka

import eco.analytics.GenericResult
import eco.analytics.mapper
import eco.analytics.rabbit.RabbitConsumerBare
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.StringSerializer
import org.fissore.slf4j.FluentLoggerFactory
import java.util.*


/**
 *
 */
class KafkaPublisherBare<T>(
        private val clazz: Class<T>
) {

    companion object {
        private val log = FluentLoggerFactory.getLogger(KafkaPublisherBare::class.java)
    }

    val compressionType: String = "LZ4"

    //    bootstrapServer 10.37.240.46:9094
    private lateinit var topic: String
    private lateinit var bootstrapServer: String

    // The producer is thread safe and sharing a single producer instance across threads will generally be faster than
    // having multiple instances.
    private lateinit var aKafkaProducer: KafkaProducer<String, String>

    fun connect(config_bootstrapServer: String, config_topic: String) {
        bootstrapServer = config_bootstrapServer
        topic = config_topic
        aKafkaProducer = KafkaProducer<String, String>(producerProps(bootstrapServer))
    }


    suspend fun publishFlow(aFlow: Flow<T>) : GenericResult<String> {
        return try {
            aFlow.collect { item ->
                aKafkaProducer.send(producerRecord(item)) { metadata: RecordMetadata, e: Exception? ->
                    e?.let {
                        e.printStackTrace()
                    }  ?:  log.trace().log("The offset of the record we just sent is: " + metadata.offset())
                }
            }
            GenericResult.Success("OK")
        } catch (e: Exception) {
            GenericResult.Failure( "NOK", e)
        }
    }


    // Note that callbacks will generally execute in the I/O thread of the producer and so should be reasonably fast or
    // they will delay the sending of messages from other threads.
    fun publish(message: T): GenericResult<String> {
        return try {
            aKafkaProducer.send(producerRecord(message)) { metadata: RecordMetadata, e: Exception? ->
                e?.let {
                    e.printStackTrace()
                } // ?:  println("The offset of the record we just sent is: " + metadata.offset())
            }
            GenericResult.Success("OK")
        } catch (e: Exception) {
            GenericResult.Failure("NOK", e)
        }
    }

    // Use flows here to allow back-pressure and suspending (async)
    // Notice how errors are guaranteed not to propagate out of here.
    @InternalCoroutinesApi
    suspend fun publish(paragonFlow: Flow<T>): GenericResult<String> {
        return try {
            paragonFlow.collect { publish(it) }
            GenericResult.Success("OK")
        } catch (e: Exception) {
            GenericResult.Failure("NOK", e)
        }
    }

    private fun producerRecord(value: T): ProducerRecord<String, String> {
//        return ProducerRecord(topic, key, value)
        val json = mapper.writeValueAsString(value)
        return ProducerRecord(topic, json)
//        return ProducerRecord(topic, json)
    }

    private fun producerProps(bootstrapServer: String): Properties {
        val serializer = StringSerializer::class.java.canonicalName
        val props = Properties()
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer)
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, serializer)
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, serializer)
        return props
    }

    //Note: after creating a KafkaProducer you must always close() it to avoid resource leaks.
    fun close() {
        aKafkaProducer?.close()
    }

}
