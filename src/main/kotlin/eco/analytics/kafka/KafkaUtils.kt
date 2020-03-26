package eco.analytics.kafka

class KafkaMessage(
        val key: String,
        val value: String
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
        var kafkaTopic: String = ""
) : java.io.Serializable {
//    constructor(params: ParameterTool) : this(
//            params.get("bootstrapServer", "localhost:9092"),
//            params.get("kafkaTopic", "flink-kafka-publish")
//    )
}
