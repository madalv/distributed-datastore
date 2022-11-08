package madalv

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json
import madalv.plugins.*
import java.io.File


val configJson: String =
    File("config/config.json").inputStream().readBytes().toString(Charsets.UTF_8)

val cfg: Config = Json.decodeFromString(Config.serializer(), configJson)

fun main() {
    embeddedServer(Netty, port = cfg.httpPort, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureSockets()
    configureRouting()
}
