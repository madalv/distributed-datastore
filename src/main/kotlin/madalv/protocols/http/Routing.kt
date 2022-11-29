package madalv.protocols.http

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import madalv.datastore.DatastoreRequest
import madalv.datastore.JumpHash
import madalv.message.MessageType
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
                if (node.isLeader()) {
                    runBlocking {
                        if ((0..2).random() == 1) redirectCreate(call)
                        else handleCreate(call)
                    }
                } else if (call.request.header("Leader-Redirect") == "true") {
                    runBlocking {
                        handleCreate(call)
                    }
                }
                else {
                    println("${node.id} IS NOT LEADER, WHO THE HELL IS SENDING HTTP REQUESTS")
                }
            }

            get("/read/{id}") {
                if (node.isLeader()) {
                    handleGet(call)
                } else try {
                    val uuid: UUID = UUID.fromString(call.parameters["id"])
                    call.respondText(String(node.datastore.read(uuid), Charset.defaultCharset()))
                } catch (_: Exception) {
                    println("couldn't get data")
                }
            }

            put("/update/{id}") {
                launch {
                    if (node.isLeader()) {
                        handleUpdate(call)
                    } else {
                        println("${node.id} IS NOT LEADER, WHO THE HELL IS SENDING HTTP REQUESTS")
                    }
                }
            }

            delete("/delete/{id}") {
                launch {
                    if (node.isLeader()) {
                        handleDelete(call)
                    } else {
                        println("${node.id} IS NOT LEADER, WHO THE HELL IS SENDING HTTP REQUESTS")
                    }
                }
            }
        }
    }
}

val client = HttpClient(CIO) {
    expectSuccess = true
}

