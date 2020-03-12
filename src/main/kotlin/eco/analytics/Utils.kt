package eco.analytics

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.quarkus.jackson.ObjectMapperCustomizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import javax.inject.Singleton


//@Singleton
//class RegisterCustomModuleCustomizer : ObjectMapperCustomizer {
//    override fun customize(mapper: ObjectMapper) {
//        mapper.registerModule(CustomModule())
//    }
//}

const val ELEMENT_DATA_ADDR= "elementData.source"
const val ELEMENT_DATA_MODEL_ADDR = "elementData.model"


fun <T> CoroutineScope.flatten(channel: ReceiveChannel<List<T>>): ReceiveChannel<T> = produce {
    for (items in channel) {
        for(item in items) send(item)
    }
}

val mapper = jacksonObjectMapper()
        .registerModule(Jdk8Module())
        .registerModule(JavaTimeModule()) // new module, NOT JSR310Module
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)



sealed class GenericResult<R> {
    data class Success<R>(val result: R): GenericResult<R>()
    data class Failure<R>(val message: String, val cause: Exception? = null) : GenericResult<R>()
}

