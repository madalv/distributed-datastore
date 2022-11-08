package madalv.protocols.udp

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import madalv.cfg
import madalv.protocols.tcp.TCP

object UDP {

    private val selectorManager = ActorSelectorManager(Dispatchers.IO)
    private val DefaultPort = cfg.udpPort

    object Server {
        @JvmStatic
        fun main() {
            runBlocking {
                val serverSocket = aSocket(selectorManager).udp().bind(InetSocketAddress("0.0.0.0", DefaultPort))
                println("UDP Server listening at ${serverSocket.localAddress}")

                for (datagram in serverSocket.incoming) {
                    println(datagram.packet.readText())
                }
            }
        }
    }
}