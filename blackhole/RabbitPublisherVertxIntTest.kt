package eco.analytics.rabbit

import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Integration Test - RabbitPublisherVertxIntTest
 *  Depends on RabbitMQ instance running, amqp://guest:guest@localhost:5672
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RabbitPublisherVertxIntTest {

    val exchangeName = "eco-analytics-test-publish"
    val queueName = "eco-analytics-test-queue"
    val uri = "amqp://guest:guest@10.37.240.51:5672"


    val consumerThread = newSingleThreadContext("consumerThread")

//    lateinit var job : Job
//    lateinit var scope : CoroutineScope

    //________________________________________________________________________________
    // BEFORE EACH
    //________________________________________________________________________________
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
                val aRabbitConsumerVertx = RabbitConsumerVertx()
                val channel = aRabbitConsumerVertx.connect(uri, exchangeName, queueName)
                for(buf in channel) {
                    latch.countDown()
                }
            }

            val aRabbitPublisherVertx = RabbitPublisherVertx()
            aRabbitPublisherVertx.connect(uri, exchangeName)
            repeat(messageCount) {
//                println("publising ... $it")
                aRabbitPublisherVertx.publishWithConfirm(genJsonObject())
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

}