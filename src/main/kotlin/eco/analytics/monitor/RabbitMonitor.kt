package eco.analytics.monitor

import eco.analytics.ELEMENT_DATA_ADDR
import eco.analytics.ELEMENT_DATA_MODEL_ADDR
import eco.analytics.mapper
import eco.analytics.rabbit.ElementDataModel
import eco.analytics.rabbit.ElementProperty
import eco.model.ElementData
import io.quarkus.vertx.ConsumeEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import org.fissore.slf4j.FluentLoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.regex.Pattern
import javax.enterprise.context.ApplicationScoped


/**
 *
 */
@ApplicationScoped
open class RabbitMonitor {

    companion object {
        private val log = FluentLoggerFactory.getLogger(RabbitMonitor::class.java)

    }

    val isNumberPattern : Pattern = Pattern.compile("^[0-9]+$")

    // Thread safe within context
    var elementDataCount: Long = 0L
    var elementDataModel  = mutableMapOf<String, ElementProperty>()


    val job = Job()
    val context = newSingleThreadContext("rabbit-monitor")
    val scope = CoroutineScope(context + job)


    @ConsumeEvent(ELEMENT_DATA_ADDR)
    fun consumeElementData(bytes: ByteArray) {
        scope.launch {
            val aElementData = mapper.readValue(bytes, ElementData::class.java)
            receiveElementData(aElementData)
        }
    }

    @ConsumeEvent(ELEMENT_DATA_MODEL_ADDR)
    fun elementDataModel(ts: Long) : CompletionStage<ElementDataModel> {
        val future = CompletableFuture<ElementDataModel>()
        scope.launch {
            val aElementDataModel = ElementDataModel(elementDataCount, elementDataModel.toMap())
            future.complete(aElementDataModel)
        }
        return future
    }

    suspend fun receiveElementData(aElementData : ElementData) {
        elementDataCount++
        val properties = aElementData.properties()
        val ts = aElementData.ts()
        properties.keys.forEach { key ->
            val value = properties[key].toString()
            elementDataModel[key]?.let {
                updateElementProperty(value, it)
                it.updated = ts
            } ?: run {
                val newElementProperty = createElementProperty(value)
                newElementProperty.updated = ts
                elementDataModel[key] = newElementProperty
            }
        }
    }

    fun createElementProperty(value: String) : ElementProperty {
        return if(isNumberPattern.matcher(value).find()) {
            receiveNewNumber(value)
        } else {
            receiveNewString(value)
        }
    }

    fun updateElementProperty(value: String, aElementProperty: ElementProperty) {
        if(aElementProperty.isNumber) {
            try {
                receiveExistingNumber(value, aElementProperty)
            } catch(e: Throwable) {
                log.warn().withCause(e).log("avg=${aElementProperty.avg}, current=${value}")
            }
        } else {
            receiveExistingString(value, aElementProperty)
        }
    }

    fun receiveExistingNumber(value: String, aElementProperty: ElementProperty) {
        aElementProperty.count++
        aElementProperty.min = Math.min(aElementProperty.min, value.toDouble())
        aElementProperty.max = Math.max(aElementProperty.max, value.toDouble())
        aElementProperty.sum = aElementProperty.sum +  value.toDouble()
        aElementProperty.avg =  aElementProperty.sum / aElementProperty.count
    }

    fun receiveExistingString(value: String, aElementProperty: ElementProperty) {
        aElementProperty.count++
        aElementProperty.values.add(value)
    }

    fun receiveNewNumber(value: String)  : ElementProperty {
        return ElementProperty(
                isNumber = true,
                count = 1.0,
                min = value.toDouble(),
                max = value.toDouble(),
                avg = value.toDouble(),
                values = mutableSetOf(value)
        )
    }

    fun receiveNewString(value: String)  : ElementProperty {
        return ElementProperty(isNumber = false, count = 1.0, values = mutableSetOf(value))
    }

} // RabbitMonitor


