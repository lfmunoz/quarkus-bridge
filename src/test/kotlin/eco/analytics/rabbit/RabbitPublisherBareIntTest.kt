package eco.analytics.rabbit

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random


/**
 * Integration Test - RabbitPublisherVertxIntTest
 *  Depends on RabbitMQ instance running, amqp://guest:guest@localhost:5672
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RabbitPublisherBareIntTest {

    private val exchangeName = "eco-analytics-test-publish"
    private val queueName = "eco-analytics-test-queue"
    private val uri = "amqp://guest:guest@10.37.240.51:5672"
    private val consumerThread = newSingleThreadContext("consumerThread")

    //________________________________________________________________________________
    // BEFORE EACH
    //________________________________________________________________________________
    @BeforeAll
    fun beforeAll() {
        logLevel("io.netty", Level.INFO)
//        logLevel("io.netty.util.internal.PlatformDependent0", Level.INFO)
//        logLevel("io.netty.util.internal.PlatformDependent0", Level.INFO)
    }
    @BeforeEach
    fun beforeEach() {
//        scope = CoroutineScope(consumerThread)
    }
    @AfterEach
    fun afterEach() {
//        scope = CoroutineScope(consumerThread)
    }

    //________________________________________________________________________________
    // TEST CASES
    //________________________________________________________________________________
    @FlowPreview
    @Test
    fun basicPublishing() {
        val messageCount = 15
        val latch = CountDownLatch(messageCount)
        runBlocking {
            launch(consumerThread) {
                val aRabbitConsumerVertx = RabbitConsumerBare(uri)
                val flow = aRabbitConsumerVertx.connect(exchangeName, queueName)
                flow.take(messageCount).collect {
//                    println(String(it))
                    latch.countDown()
                }
            }
            val aRabbitPublisherBare = RabbitPublisherBare(uri)
            aRabbitPublisherBare.connect(exchangeName)
            repeat(messageCount) {
                aRabbitPublisherBare.publish(genJsonObject())
            }
        }
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()

    }
    //________________________________________________________________________________
    // PRIVATE HELPER METHODS
    //________________________________________________________________________________
    private fun genJsonObject() : JsonObject {
        val random = Random.nextInt(100000, 999999)
        return json {
            obj("body" to "Hello RabbitMQ, from Vert.x !")
        }
    }


    private fun logLevel(path: String, aLevel: Level) {
        val logger: Logger = LoggerFactory.getLogger(path) as Logger
        logger.setLevel(aLevel)
    }


}