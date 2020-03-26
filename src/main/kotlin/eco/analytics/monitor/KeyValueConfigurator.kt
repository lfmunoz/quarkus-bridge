package eco.analytics.monitor

import com.fasterxml.jackson.databind.ObjectMapper
import eco.analytics.mapper
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import org.fissore.slf4j.FluentLoggerFactory
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/api")
open class KeyValueConfigurator(
) {

    private val log = FluentLoggerFactory.getLogger(KeyValueConfigurator::class.java)

    // TODO: value should be some sort of container object with value, type, and other info
    val keyValueMap: Map<String, String> = mapOf(
            "key1" to "1",
            "key2" to "2"
    )


    init {

    }

    @GET
    @Path("/set")
    @Produces(MediaType.APPLICATION_JSON)
    fun set(): JsonObject {
        log.debug().log("[set]")
        return JSON_OK
    }

    @GET
    @Path("/view")
    @Produces(MediaType.APPLICATION_JSON)
    fun inspect(): String {
        log.debug().log("[inspect]")

        return mapper.writeValueAsString(keyValueMap)
    }


}

val OK = "OK"
val NOK = "NOK"

val JSON_OK = json{obj("return" to OK)}
val JSON_NOK = json{obj("return" to NOK)}
