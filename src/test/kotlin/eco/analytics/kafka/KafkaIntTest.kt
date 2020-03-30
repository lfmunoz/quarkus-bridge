package eco.analytics.kafka

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.fasterxml.jackson.module.kotlin.readValue
import eco.analytics.GenericResult
import eco.analytics.rabbit.RabbitConsumerBare
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * Integration Test:  Kafka
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KafkaIntTest {

    // Dependencies
    private val consumerThread = newFixedThreadPoolContext(4, "consumerThread")
//    private val scope = CoroutineScope(tPool)


//    private val bootstrapServer: String = "oc112-22.maas.auslab.2wire.com:9092"
    private val bootstrapServer: String = "10.37.240.46:9094"
    private val groupId: String = "analytics-bridge"
    private val topic: String = "analytics-bridge-test"

    // Unit Under Test
    lateinit var aKafkaPublisherBare: KafkaPublisherBare
    lateinit var aKafkaConsumerBare: KafkaConsumerBare<String>

    // batch.size = 16384
    // buffer.memory = 33554432
    // compression.type = none


    //________________________________________________________________________________
    // setUp and tearDown functions are ran before and after each @Test
    //________________________________________________________________________________
    @BeforeAll
    fun before() {
        changeLogLevel("org.apache.kafka.common.metrics.Metrics")
        changeLogLevel("org.apache.kafka.clients.producer.ProducerConfig")
        changeLogLevel("org.apache.kafka.clients.producer.KafkaProducer")
        changeLogLevel("org.apache.kafka.clients.consumer.ConsumerConfig")
        changeLogLevel("org.apache.kafka.clients.consumer.KafkaConsumer")
        changeLogLevel("org.apache.kafka.common.utils.AppInfoParser")
        changeLogLevel("org.apache.kafka.clients.NetworkClient")
        changeLogLevel("org.apache.kafka.clients.Metadata")
        changeLogLevel("org.apache.kafka.common.network.Selector")
    }

    private fun changeLogLevel(path: String, level: Level = Level.WARN) {
        val logger = LoggerFactory.getLogger(path) as Logger
        logger.setLevel(level)
    }

    @BeforeEach
    fun setUp() {
//        aKafkaPublisherBare = KafkaPublisherBare()
//        aKafkaConsumerBare = KafkaConsumerBare(String::class.java)
    }

    @AfterEach
    fun takeDown() {
        aKafkaPublisherBare.close()
        aKafkaConsumerBare.close()
    }

    //________________________________________________________________________________
    // Tests
    //________________________________________________________________________________
    @Disabled
    @Test
    fun `deserialize and serialize`() {
//        val obj = newParagonMessage()
//        val json = mapper.writeValueAsString(obj)
//        val objRead = mapper.readValue<ParagonMessage>(json)
//        Assertions.assertThat(obj).isEqualTo(obj)
//        genJsonObject().toString()
        assertThat(true).isTrue()
    }


    /*
    @InternalCoroutinesApi
    @Test
    fun `simple publish and consume`() {
        val totalMessages = 1_0
        val latch = CountDownLatch(totalMessages)
        runBlocking {
            launch(consumerThread) {
                val flow = aKafkaConsumerBare.connect(bootstrapServer, groupId, topic)
                flow.take(totalMessages).collect {
                    println(it)
                    latch.countDown()
                }
            }
            aKafkaPublisherBare.connect(bootstrapServer, topic)
            aKafkaPublisherBare.publishFlow(flow {
                repeat(totalMessages) {
                    emit(genJsonObject().toString())
                }
            })
        }
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
    }
*/

    @Disabled
    @Test
    fun `publishing with 4 threads`() {
        val totalMessages = 10_000_000
        val start = System.currentTimeMillis()
        val parallelism = 4
        runBlocking {
            //            newFixedThreadPoolContext(parallelism, "publisherThreadPool").use {
//                withContext(it) {
            repeat(totalMessages / parallelism) {
                //                        val result = topicPublisher.publish(newParagonMessage())
//                        Assertions.assertThat(result).isEqualTo(GenericResult.Success())
            }
//                }
//            }
        }
        val stop = System.currentTimeMillis()
        println("--------------------------------------------------------------------------")
        println("Results: ")
        println("--------------------------------------------------------------------------")
        println("Published ${totalMessages} in ${(stop - start)}  milliseconds  (rate = ${totalMessages / (stop - start)})")
        println()
    }


    //    @DisplayName("Vary number ofThreads")
    @Disabled
    @ParameterizedTest
    @ValueSource(ints = [2, 4, 6, 8, 10, 12, 14])
    fun `publishing with variable threads`(param: Int) {
        val totalMessages = 10_000_000
        val parallelism = param
        val start = System.currentTimeMillis()
        runBlocking {
            //            newFixedThreadPoolContext(parallelism, "publisherThreadPool").use {
//                withContext(it) {
            repeat(totalMessages / parallelism) {
                //                        val result = topicPublisher.publish(genJsonObject().toString())
//                        Assertions.assertThat(result).isEqualTo(GenericResult.Success())
            }
//                }
//            }
        }
        val stop = System.currentTimeMillis()
        println("--------------------------------------------------------------------------")
        println("Results: ")
        println("--------------------------------------------------------------------------")
        println("Published ${totalMessages} in ${(stop - start)}  milliseconds  (rate = ${totalMessages / (stop - start)})")
        println()
    }


    //________________________________________________________________________________
    // Helper methods
    //________________________________________________________________________________
    private val atomicId = AtomicLong()

    private fun genJsonObject(): JsonObject {
        return json {
            obj("body" to "[${atomicId.getAndIncrement()}] - Hello RabbitMQ, from Vert.x")
        }
    }
}



