package madalv.protocols.http

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import madalv.datastore.DatastoreRequest
import madalv.datastore.JumpHash
import madalv.log.LogEntry
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
                    val uuid = UUID.randomUUID()
                    val ogNode = JumpHash.hash(uuid.toString(), node.cluster.size + 1)
                    val replicaNode = JumpHash.getDuplicateId(uuid.toString(), node.cluster.size + 1)
                    println("Creating copies of data in nodes $ogNode and ${replicaNode}; $uuid")
                    val dr = DatastoreRequest(uuid, data)
                    node.executeRequest(setOf(ogNode, replicaNode), MessageType.CREATE_REQUEST, dr)

                } else {
                    println("${node.id} IS NOT LEADER, WHO THE HELL IS SENDING HTTP REQUESTS")
                }
            }

            get("/read/{id}") {
                val uuid: UUID = UUID.fromString(call.parameters["id"])
                if (node.isLeader()) {
                    val ogNode = JumpHash.hash(uuid.toString(), node.cluster.size + 1)
                    val replicaNode = JumpHash.getDuplicateId(uuid.toString(), node.cluster.size + 1)

                    println("Reaching for data in nodes $ogNode and $replicaNode")

                    if (ogNode == node.id) {
                        call.respondText(String(node.datastore.read(uuid), Charset.defaultCharset()))
                    } else {
                        val n = node.cluster[ogNode]!!
                        var data: ByteArray = "Hz".toByteArray()

                        // todo refactor this bs (+ extend jump hash for 4+ nodes)
                        GlobalScope.launch {
                            try {
                                data = client.get("http://${n.host}:${n.httpPort}/ds/read/${uuid}").body()
                            } catch (e: Exception) {
                                try {
                                    if (replicaNode == node.id) {
                                        call.respondText(String(node.datastore.read(uuid), Charset.defaultCharset()))
                                    } else {
                                        val o = node.cluster[replicaNode]!!
                                        data = client.get("http://${o.host}:${o.httpPort}/ds/read/${uuid}").body()
                                    }
                                } catch (e: Exception) {
                                    println(e.message)
                                }
                            }
                        }.join()
                        call.respondText(String(data, Charset.defaultCharset()))
                    }
                } else {
                    call.respondText(String(node.datastore.read(uuid), Charset.defaultCharset()))
                }
            }

            put("/update/{id}") {
                if (node.isLeader()) {
                    val uuid: UUID = UUID.fromString(call.parameters["id"])
                    val ogNode = JumpHash.hash(uuid.toString(), node.cluster.size + 1)
                    val replicaNode = JumpHash.getDuplicateId(uuid.toString(), node.cluster.size + 1)
                    val data: ByteArray = call.receive()
                    val dr = DatastoreRequest(uuid, data)
                    node.executeRequest(setOf(ogNode, replicaNode), MessageType.UPDATE_REQUEST, dr)
                } else {
                    println("${node.id} IS NOT LEADER, WHO THE HELL IS SENDING HTTP REQUESTS")
                }
            }

            delete("/delete/{id}") {
                if (node.isLeader()) {
                    val uuid: UUID = UUID.fromString(call.parameters["id"])
                    val dr = DatastoreRequest(uuid)
                    val ogNode = JumpHash.hash(uuid.toString(), node.cluster.size + 1)
                    val replicaNode = JumpHash.getDuplicateId(uuid.toString(), node.cluster.size + 1)
                    node.executeRequest(setOf(ogNode, replicaNode), MessageType.DELETE_REQUEST, dr)
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