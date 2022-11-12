package madalv.protocols.http

import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import madalv.node

fun Application.configureRouting() {

    routing {
        get("/") {
            call.respondText("Hello World ${node.id}!")
        }
    }
}
