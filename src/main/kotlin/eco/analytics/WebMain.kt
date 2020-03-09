package eco.analytics

import eco.analytics.rabbit.RabbitConsumerVertx
import kotlinx.coroutines.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType


@Path("/api")
class WebMain {

    var isNumberPattern : Pattern = Pattern.compile("^[0-9]+$")


    @Inject
    lateinit var aRabbitConsumerVertx: RabbitConsumerVertx
//
    val job = Job()
    val context = newSingleThreadContext("elementDataView")
    val scope = CoroutineScope(context + job)

    // Must be in context thread
    var elementDataCount: Int = 0
    var elementDataModel  = mutableMapOf<String, ElementProperty>()

    init {
        aggregateModel()
    }

    fun aggregateModel() {
        scope.launch {
            while(isActive) {
//                val channel = aRabbitConsumerVertx.aReceiveChannel ?: continue
                for(elementData in aRabbitConsumerVertx.aReceiveChannel) {
                    elementData.forEach {
                        elementDataCount++
                        val properties = it.properties()
                        val ts = it.ts()
                        properties.keys.forEach { key ->
                            if(elementDataModel.containsKey(key)) {
                                processExistingKey(properties[key].toString(), elementDataModel[key]!!)
                                elementDataModel[key]?.updated = ts
                            } else {
                               val newElement = processNewKey(key, properties[key].toString(), ts)
                                elementDataModel.put(key,newElement)
                            }
                        }
                    }
                }
            }
        }
    }

    fun processExistingKey(value: String, aElementProperty: ElementProperty) {
        if(aElementProperty.isNumber) {
            aElementProperty.seen++
            aElementProperty.min = Math.min(aElementProperty.min, value.toDouble())
            aElementProperty.max = Math.max(aElementProperty.max, value.toDouble())
            // TODO: handle roll over of sum
            aElementProperty.sum = aElementProperty.sum +  value.toDouble()
            aElementProperty.avg =   aElementProperty.sum / aElementProperty.seen
        } else {
            aElementProperty.seen++
            aElementProperty.values.add(value)
        }
    }

    fun processNewKey(key: String, value: String, ts: Long) : ElementProperty {
        return if(isNumberPattern.matcher(value).find()) {
            ElementProperty(
                isNumber= true,
                updated = ts,
                seen = 0,
                min = 0.0,
                max = 0.0,
                avg =  0.0,
                values = mutableSetOf()
            )
        } else {
            ElementProperty(
                    isNumber= false,
                    updated = ts,
                    seen = 0,
                    min = 0.0,
                    max = 0.0,
                    avg =  0.0,
                    values = mutableSetOf(value)
            )
        }
    }

//    fun currentModel() : Json {
//
//    }

//    @GET
//    @Produces(MediaType.TEXT_PLAIN)
//    fun hello() = "hello"

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun rabbitPacket() : CompletionStage<ElementDataModel> {
        val future = CompletableFuture<ElementDataModel>()
        scope.launch {
            future.complete(ElementDataModel(elementDataCount, elementDataModel))
        }
        return future
    }

}

data class ElementDataModel (
        val receiveCount: Int? = null,
        val model: Map<String, ElementProperty>? = null
) {

    constructor() : this(null, null) {}
}

data class ElementProperty(
        var isNumber: Boolean = true,
        var updated: Long = 0,
        var seen: Long = 0,
        var sum: Double = 0.0,
        var min: Double = 0.0,
        var max: Double = 0.0,
        var avg: Double = 0.0,
        val values: MutableSet<String>  = mutableSetOf()
) {
    constructor() : this(true)
}

