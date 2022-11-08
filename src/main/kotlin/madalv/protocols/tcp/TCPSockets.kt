package madalv.protocols.tcp

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import madalv.cfg

object TCP {
    private val selectorManager = ActorSelectorManager(Dispatchers.IO)
    private val DefaultPort = cfg.tcpPort


    object Server {
        @JvmStatic
        fun main() {
            runBlocking {
                val serverSocket = aSocket(selectorManager).tcp().bind(InetSocketAddress("0.0.0.0", DefaultPort))
                println("TCP Server listening at ${serverSocket.localAddress}")
                while (true) {
                    val socket = serverSocket.accept()
                    println("Accepted $socket")
                    launch {
                        val read = socket.openReadChannel()
                        val write = socket.openWriteChannel(autoFlush = true)
                        try {
                            while (true) {
                                val line = read.readUTF8Line()
                                println("server: $line")
                                write.writeStringUtf8("$line\n")
                            }
                        } catch (e: Throwable) {
                            socket.close()
                        }
                    }
                }
            }
        }
    }

    object Client {
        @JvmStatic
        fun main() {
            runBlocking {
                val socket = aSocket(selectorManager).tcp().connect("0.0.0.0", port = DefaultPort)
                val read = socket.openReadChannel()
                val write = socket.openWriteChannel(autoFlush = true)
                val lines = listOf("a", "b", "c")

                for (line in lines) {
                    println("client: $line")
                    write.writeStringUtf8("$line\n")
                }
            }
        }
    }
}