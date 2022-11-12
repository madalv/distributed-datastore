package madalv.protocols.tcp

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
                val serverSocket = aSocket(selectorManager).tcp().bind(InetSocketAddress("0.0.0.0", DefaultPort))
                println("TCP Server listening at ${serverSocket.localAddress}")

                while (true) {
                    val socket = serverSocket.accept()
                    val read = socket.openReadChannel()
                    println("Accepted $socket")
                    launch {
                        try {
                            while (true) {
                                val line = read.readUTF8Line()
                                val message: Message = Json.decodeFromString(Message.serializer(), line!!)

                                when(message.messageType) {
                                    MessageType.VOTE_REQUEST -> {
                                        println("ERROR - why sending vote request (broadcast) thru TCP?")
                                    }
                                    MessageType.VOTE_RESPONSE -> {
                                        val vr = Json.decodeFromString(VoteResponse.serializer(), message.data)
                                        println(" INCOMING VOTE REPSPONSE " + message.data)
                                        node.electionManager.voteResponseChannel.send(vr)
                                    }
                                    else -> {

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
//        fun main() {
//            runBlocking {
//                val socket = aSocket(selectorManager).tcp().connect("0.0.0.0", port = DefaultPort)
//                val read = socket.openReadChannel()
//                val write = socket.openWriteChannel(autoFlush = true)
//                val lines = listOf("a", "b", "c")
//
//                for (line in lines) {
//                    println("client: $line")
//                    write.writeStringUtf8("$line\n")
//                }
//            }
//        }


        fun send(address: InetSocketAddress, message: String) {
            runBlocking {
                //println("${address.hostname} ${address.port} $message")
                val socket = aSocket(selectorManager).tcp().connect(address)
                //val read = socket.openReadChannel()
                val write = socket.openWriteChannel(autoFlush = true)
                write.writeStringUtf8(message + "\n")
            }
        }
    }
}