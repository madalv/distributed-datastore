package madalv

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import madalv.node.Config
import madalv.node.Node
import madalv.plugins.*
import madalv.protocols.http.configureRouting
import madalv.protocols.tcp.TCP
import madalv.protocols.udp.UDP
import java.io.File


val node = init()

// TODO start timer for leader timeout
// TODO implement rest of raft

suspend fun main() {
    // launch tcp server
    CoroutineScope(Dispatchers.Default).launch {
        TCP.Server.start()
    }

    // launch udp server
    CoroutineScope(Dispatchers.Default).launch {
        UDP.Server.start()
    }

    // launch http server
    embeddedServer(Netty, port = node.httpPort, host = node.host, module = Application::module).start(wait = false)

    delay(node.electionManager.electionTimeout!!)
    if (!node.leaderExists()) node.electionManager.initElection()
}

fun Application.module() {
    configureSerialization()
    configureSockets()
    configureRouting()
}
fun init(): Node {
    val configJson: String = File("config/config1/config.json").inputStream().readBytes().toString(Charsets.UTF_8)
    val nodesJson: String = File("nodes/nodes.json").inputStream().readBytes().toString(Charsets.UTF_8)
    val cfg: Config = Json.decodeFromString(Config.serializer(), configJson)
    val nodes: Map<Int, Node> = Json.decodeFromString(nodesJson)
    val node: Node = nodes.getValue(cfg.id)
    node.setCluster(nodes)
    node.electionManager.electionTimeout = cfg.electionTimeout
    return node
}