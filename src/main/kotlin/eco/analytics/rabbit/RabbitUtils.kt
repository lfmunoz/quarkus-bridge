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
