package eco.analytics

import com.fasterxml.jackson.databind.ObjectMapper
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



fun <T> CoroutineScope.flatten(channel: ReceiveChannel<List<T>>): ReceiveChannel<T> = produce {
    for (items in channel) {
        for(item in items) send(item)
    }
}