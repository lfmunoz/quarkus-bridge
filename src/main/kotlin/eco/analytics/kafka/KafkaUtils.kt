package eco.analytics.kafka

class KafkaMessage(
        val key: ByteArray,
        val value: ByteArray
)

//data class KafkaMessage(
//        val key: ByteArray,
//        val value: ByteArray
//) : java.io.Serializable {
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (javaClass != other?.javaClass) return false
//        other as KafkaMessage
//        if (!key.contentEquals(other.key)) return false
//        return true
//    }
//
//    override fun hashCode(): Int {
//        return key.contentHashCode()
//    }
//}

//________________________________________________________________________________
// KAFKA CONFIG
//________________________________________________________________________________
data class KafkaConfig(
        var bootstrapServer: String = "",
        var topic: String = "",
        var compression: String = "LZ4"
) : java.io.Serializable {
//    constructor(params: ParameterTool) : this(
//            params.get("bootstrapServer", "localhost:9092"),
//            params.get("kafkaTopic", "flink-kafka-publish")
//    )
}
