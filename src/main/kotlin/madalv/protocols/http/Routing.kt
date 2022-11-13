package madalv.protocols.http

import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import madalv.node
import java.nio.charset.Charset
import java.util.*

fun Application.configureRouting() {

    routing {
        get("/") {
            call.respondText("Hello World ${node.id}!")
        }

        route("/ds") {

            post("/create") {
                if (true) {
                    val data: ByteArray = call.receive()
                    node.datastore.create(data)
                } else {
                    println("${node.id} IS NOT LEADER, WHO THE HELL IS SENDING HTTP REQUESTS")
                }
            }

            get("/read/{id}") {
                if (true) {
                    val id: UUID = UUID.fromString(call.parameters["id"])
                    call.respondText(String(node.datastore.read(id), Charset.defaultCharset()))
                } else {
                    println("${node.id} IS NOT LEADER, WHO THE HELL IS SENDING HTTP REQUESTS")
                }
            }

            put("/update/{id}") {
                if (true) {
                    val id: UUID = UUID.fromString(call.parameters["id"])
                    val data: ByteArray = call.receive()
                    node.datastore.update(id, data)
                } else {
                    println("${node.id} IS NOT LEADER, WHO THE HELL IS SENDING HTTP REQUESTS")
                }
            }

            delete("/delete/{id}") {
                if (true) {
                    val id: UUID = UUID.fromString(call.parameters["id"])
                    node.datastore.delete(id)
                } else {
                    println("${node.id} IS NOT LEADER, WHO THE HELL IS SENDING HTTP REQUESTS")
                }
            }
        }
    }
}
