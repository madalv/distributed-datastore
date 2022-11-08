package madalv

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import madalv.models.Config
import madalv.plugins.*
import madalv.protocols.tcp.TCP
import madalv.protocols.udp.UDP
import java.io.File


val configJson: String =
    File("config1/config.json").inputStream().readBytes().toString(Charsets.UTF_8)

val cfg: Config = Json.decodeFromString(Config.serializer(), configJson)
// TODO connect to list of instances
// TODO HTTP CRUD interface
// TODO add datastore
// TODO partition leader assignment

fun main() {
    // launch tcp server
    CoroutineScope(Dispatchers.Default).launch {
        TCP.Server.main()
    }

    // launch udp server
    CoroutineScope(Dispatchers.Default).launch {
        UDP.Server.main()
    }

    // launch http server
    embeddedServer(Netty, port = cfg.httpPort, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureSockets()
    configureRouting()
}
