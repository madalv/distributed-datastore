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
    @SerialName("suffix") val suffix: Int
)

@Serializable
data class LogEntry(
    @SerialName("message") val msg: Message,
    @SerialName("term") val term: Int
)