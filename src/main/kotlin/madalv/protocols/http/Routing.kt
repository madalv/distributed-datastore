package madalv.protocols.http

import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import kotlinx.serialization.json.Json
import madalv.datastore.DatastoreRequest
import madalv.message.Message
import madalv.message.MessageType
import madalv.node
import java.nio.charset.Charset
import java.util.*
import kotlin.random.Random

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
                    call.respondText(String(node.datastore.read(id), Charset.defaultCharset()))
//                    val dr = DatastoreRequest(id)
//                    val message = Message(MessageType.READ_REQUEST, Json.encodeToString(DatastoreRequest.serializer(), dr))
                    val nodeId = Random(System.currentTimeMillis()).nextInt(0, 3)
                    // TODO add http client get
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
