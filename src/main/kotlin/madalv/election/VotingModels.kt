package madalv.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoteRequest(
    @SerialName("erm") val term: Int,
    @SerialName("candidate_id") val candidateId: Int,
    @SerialName("log_length") val logLength: Int,
    @SerialName("last_log_term") val lastLogTerm: Int
)

@Serializable
data class VoteResponse(
    @SerialName("node_id") val nodeId: Int,
    @SerialName("term") val term: Int,
    @SerialName("vote_granted") val voteGranted: Boolean
)