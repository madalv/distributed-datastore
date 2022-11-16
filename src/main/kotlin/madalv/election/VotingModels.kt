package madalv.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoteRequest(
    @SerialName("current_term") val currentTerm: Int,
    @SerialName("candidate_id") val candidateId: Int,
    @SerialName("last_log_index") val lastLogIndex: Int,
    @SerialName("last_log_term") val lastLogTerm: Int
)

@Serializable
data class VoteResponse(
    @SerialName("node_id") val nodeId: Int,
    @SerialName("current_term") val currentTerm: Int,
    @SerialName("vote_granted") val voteGranted: Boolean
)