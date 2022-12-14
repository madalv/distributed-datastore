package madalv.protocols.tcp

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import madalv.datastore.DatastoreRequest
import madalv.log.LogRequest
import madalv.message.Message
import madalv.message.MessageType
import madalv.message.VoteResponse
import madalv.node

object TCP {
    private val selectorManager = ActorSelectorManager(Dispatchers.IO)
    private val DefaultPort = node.tcpPort


    object Server {
        @JvmStatic
        fun start() {
            runBlocking {
                val serverSocket = aSocket(selectorManager).tcp().bind(InetSocketAddress(node.host, DefaultPort))
                println("TCP Server listening at ${serverSocket.localAddress}")

                while (true) {
                    val socket = serverSocket.accept()
                    val read = socket.openReadChannel()

                    launch {
                        try {
                            while (true) {
                                val line = read.readUTF8Line()
                                val message: Message = Json.decodeFromString(Message.serializer(), line!!)

                                when(message.messageType) {
                                    MessageType.VOTE_RESPONSE -> {
                                        val vr = Json.decodeFromString(VoteResponse.serializer(), message.data)
                                        println(" INCOMING VOTE REPSPONSE " + message.data)
                                        node.electionManager.voteResponseChannel.send(vr)
                                    }
                                    MessageType.UPDATE_REQUEST -> {
                                        val dr = Json.decodeFromString(DatastoreRequest.serializer(), message.data)
                                        node.datastore.update(dr.key!!, dr.data!!)
                                    }
                                    MessageType.CREATE_REQUEST -> {
                                        val dr = Json.decodeFromString(DatastoreRequest.serializer(), message.data)
                                        node.datastore.create(dr.key!!, dr.data!!)
                                    }
                                    MessageType.DELETE_REQUEST -> {
                                        val dr = Json.decodeFromString(DatastoreRequest.serializer(), message.data)
                                        node.datastore.delete(dr.key!!)
                                    }
                                    MessageType.LOG_REQUEST -> {
                                        val lr = Json.decodeFromString(LogRequest.serializer(), message.data)
                                        node.receiveLogRequest(lr)
                                    }
                                    MessageType.LOG_RESPONSE -> {
                                        val lr = Json.decodeFromString(LogRequest.serializer(), message.data)
                                        node.receiveLogRequest(lr)
                                    }
                                    else -> {
                                        println("UNKOWN MESSAGE TYPE TCP CONN: ${message.messageType}")
                                    }
                                }
                            }
                        } catch (e: Throwable) {
                            withContext(Dispatchers.IO) {
                                socket.close()
                            }
                        }
                    }
                }
            }
        }
    }

    object Client {
        @JvmStatic
        fun send(address: InetSocketAddress, message: String) {
            runBlocking {
                try {
                    val socket = aSocket(selectorManager).tcp().connect(address)
                    val write = socket.openWriteChannel(autoFlush = true)
                    write.writeStringUtf8(message + "\n")
                } catch (e: Exception) {
                    println("Couldn't send packet to ${address.hostname}:${address.port}")
                }
            }
        }
    }
}