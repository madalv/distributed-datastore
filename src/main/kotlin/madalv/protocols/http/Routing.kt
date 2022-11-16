package madalv.protocols.http

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import madalv.datastore.DatastoreRequest
import madalv.message.Message
import madalv.message.MessageType
import madalv.node
import java.nio.charset.Charset
import java.util.*
import kotlin.random.Random

@OptIn(DelicateCoroutinesApi::class)
fun Application.configureRouting() {

    routing {
        get("/") {
            call.respondText("Hello World ${node.id}!")
        }

        route("/ds") {

            post("/create") {
                if (node.isLeader()) {
                    val data: ByteArray = call.receive()
                    val uuid = node.datastore.create(data)
                    val dr = DatastoreRequest(uuid, data)
                    val message = Message(MessageType.UPDATE_REQUEST, Json.encodeToString(DatastoreRequest.serializer(), dr))
                    node.broadcast(Json.encodeToString(Message.serializer(), message))

                } else {
                    println("${node.id} IS NOT LEADER, WHO THE HELL IS SENDING HTTP REQUESTS")
                }
            }

            get("/read/{id}") {
                val id: UUID = UUID.fromString(call.parameters["id"])
                if (node.isLeader()) {
                    val nodeId = Random(System.currentTimeMillis()).nextInt(0, 3)

                    if (nodeId == node.id) {
                        call.respondText(String(node.datastore.read(id), Charset.defaultCharset()))
                    } else {
                        val n = node.cluster[nodeId]!!
                        var data: ByteArray = ByteArray(0)

                        GlobalScope.launch {
                            data = client.get("http://${n.host}:${n.httpPort}/ds/read/${id}").body()
                        }.join()

                        println("Got from node $nodeId! data = ${String(data)}")
                        call.respondText(String(data, Charset.defaultCharset()))
                    }
                } else {
                    call.respondText(String(node.datastore.read(id), Charset.defaultCharset()))
                }
            }

            put("/update/{id}") {
                if (node.isLeader()) {
                    val id: UUID = UUID.fromString(call.parameters["id"])
                    val data: ByteArray = call.receive()
                    node.datastore.update(id, data)
                    val dr = DatastoreRequest(id, data)
                    val message = Message(MessageType.UPDATE_REQUEST, Json.encodeToString(DatastoreRequest.serializer(), dr))
                    node.broadcast(Json.encodeToString(Message.serializer(), message))
                } else {
                    println("${node.id} IS NOT LEADER, WHO THE HELL IS SENDING HTTP REQUESTS")
                }
            }

            delete("/delete/{id}") {
                if (node.isLeader()) {
                    val id: UUID = UUID.fromString(call.parameters["id"])
                    node.datastore.delete(id)
                    val dr = DatastoreRequest(id)
                    val message = Message(MessageType.DELETE_REQUEST, Json.encodeToString(DatastoreRequest.serializer(), dr))
                    node.broadcast(Json.encodeToString(Message.serializer(), message))
                } else {
                    println("${node.id} IS NOT LEADER, WHO THE HELL IS SENDING HTTP REQUESTS")
                }
            }
        }
    }
}

val client = HttpClient(CIO) {
    expectSuccess = true
}


