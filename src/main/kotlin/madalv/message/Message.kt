package madalv.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message (
    @SerialName("message_type") val messageType: MessageType,
    @SerialName("data") val data: String
    ) {
    override fun toString(): String {
        return "{msg_type=$messageType}"
    }
}