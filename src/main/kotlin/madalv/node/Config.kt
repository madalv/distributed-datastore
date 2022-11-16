package madalv.node

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Config(
    @SerialName("id") val id: Int,
    @SerialName("election_timeout") val electionTimeout: Long
)