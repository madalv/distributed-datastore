package madalv.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Config(
    @SerialName("http_port") val httpPort: Int,
    @SerialName("udp_port") val udpPort: Int,
    @SerialName("tcp_port") val tcpPort: Int,
    @SerialName("host") val host: String
    )