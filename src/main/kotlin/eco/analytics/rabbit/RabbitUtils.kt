package eco.analytics.rabbit

data class ElementDataModel (
        val receiveCount: Long,
        val model: Map<String, ElementProperty>
) {

    constructor() : this(0L, mapOf()) {}
}

data class ElementProperty(
        var isNumber: Boolean = true,
        var updated: Long = 0,
        var count: Double = 0.0,
        var sum: Double = 0.0,
        var min: Double = 0.0,
        var max: Double = 0.0,
        var avg: Double = 0.0,
        val values: MutableSet<String>  = mutableSetOf()
) {
    constructor() : this(true)
}


//________________________________________________________________________________
// RABBIT CONFIG
//________________________________________________________________________________
data class RabbitConfig(
        var amqp: String = "",
        var queue: String = "",
        var exchange: String = "",
        var bufferSize: Int = 100
) : java.io.Serializable {
//    constructor(params: ParameterTool) : this(
//            params.get("amqp", "amqp://guest:guest@localhost:5672"),
//            params.get("queue", "flink-rabbit-consumer"),
//            params.get("exchange", "flink-rabbit-exchange"),
//            params.getInt("bufferSize", 100)
//    )
}

