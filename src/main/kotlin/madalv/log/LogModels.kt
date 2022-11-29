package madalv.log

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import madalv.message.Message

@Serializable
data class LogRequest(
    @SerialName("leader_id") val leaderId: Int,
    @SerialName("current_term") val currentTerm: Int,
    @SerialName("prefix_len") val prefixLen: Int,
    @SerialName("prefix_term") val prefixTerm: Int,
    @SerialName("commit_len") val commitLen: Int,
    @SerialName("suffix") val suffix: MutableList<LogEntry>
) {
    override fun toString(): String {
        return "{leader=$leaderId, term=$currentTerm, pLen=$prefixLen, pTerm=$prefixTerm, cLen=$commitLen" +
                "\n suffix=${suffix.joinToString(",", "[", "]")}}"
    }
}

@Serializable
data class LogEntry(
    @SerialName("message") val msg: Message,
    @SerialName("term") val term: Int,
    @SerialName("target_nodes") val targetNodes: Set<Int>,
    @SerialName("id") val id: Int
) {
    override fun toString(): String {
        return "{msg=$msg, term=$term, tn=$targetNodes, id=$id}"
    }
}

@Serializable
data class LogResponse(
        @SerialName("node_id") val nodeId: Int,
        @SerialName("term") val term: Int,
        @SerialName("ack") val ack: Int,
        @SerialName("granted") val granted: Boolean
    )
