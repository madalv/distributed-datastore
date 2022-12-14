package madalv.protocols.udp

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import madalv.datastore.DatastoreRequest
import madalv.message.Message
import madalv.message.MessageType
import madalv.message.VoteRequest
import madalv.node

object UDP {

    private val selectorManager = ActorSelectorManager(Dispatchers.IO)
    private val DefaultPort = node.udpPort

    object Server {
        @JvmStatic
        fun start() {
            runBlocking {
                val serverSocket = aSocket(selectorManager).udp().bind(InetSocketAddress(node.host, DefaultPort))
                println("UDP Server listening at ${serverSocket.localAddress}")

                for (datagram in serverSocket.incoming) {
                    val raw = datagram.packet.readText()
                    val message: Message = Json.decodeFromString(Message.serializer(), raw)

                    when(message.messageType) {
                        MessageType.VOTE_REQUEST -> {
                            val vr = Json.decodeFromString(VoteRequest.serializer(), message.data)
                            node.electionManager.vote(vr)
                        }
                        MessageType.UPDATE_REQUEST -> {
                            val dr = Json.decodeFromString(DatastoreRequest.serializer(), message.data)
                            node.datastore.update(dr.key!!, dr.data!!)
                        }
                        MessageType.DELETE_REQUEST -> {
                            val dr = Json.decodeFromString(DatastoreRequest.serializer(), message.data)
                            node.datastore.delete(dr.key!!)
                        }
                        else -> {
                            println("UNKOWN MESSAGE TYPE UDP CONN: ${message.messageType}")
                        }
                    }
                }
            }
        }
    }

    object Client {
        @JvmStatic
        suspend fun broadcast(message: String) {
            for (n in node.cluster) {
                try{
                    val socket = aSocket(selectorManager).udp().connect(InetSocketAddress(n.value.host, n.value.udpPort))
                    val buffer = socket.openWriteChannel(true)
                    buffer.writeStringUtf8(message)
                } catch (e: Exception) {
                    println("Couldn't send datagram to ${n.value.host}:${n.value.udpPort}")
                }
            }
        }
    }
}