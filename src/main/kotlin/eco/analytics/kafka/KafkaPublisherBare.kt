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
class KafkaPublisherBare {
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


    suspend fun publishFlow(aFlow: Flow<KafkaMessage>) : GenericResult<String> {
        return try {
            aFlow.collect { item ->
                aKafkaProducer.send(buildProducerRecord(item)) { metadata: RecordMetadata, e: Exception? ->
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
    fun publishMessage(message: KafkaMessage): GenericResult<String> {
        return try {
            aKafkaProducer.send(buildProducerRecord(message)) { metadata: RecordMetadata, e: Exception? ->
                e?.let {
                    e.printStackTrace()
                } // ?:  println("The offset of the record we just sent is: " + metadata.offset())
            }
            GenericResult.Success("OK")
        } catch (e: Exception) {
            GenericResult.Failure("NOK", e)
        }
    }

    private fun buildProducerRecord(aKafkaMessage: KafkaMessage): ProducerRecord<String, String> {
        return ProducerRecord(topic, aKafkaMessage.key, aKafkaMessage.value)
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

}// end of KafkaPublisherBare
