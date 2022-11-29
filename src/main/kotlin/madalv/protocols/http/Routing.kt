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
                        val data: ByteArray = call.receive()
                        val uuid = UUID.randomUUID()
                        val replicaNodes = JumpHash.getDuplicates(uuid.toString(), node.cluster.size + 1)
                        println("Creating copies of data in nodes $replicaNodes: $uuid")
                        val dr = DatastoreRequest(uuid, data)
                        node.executeRequest(replicaNodes, MessageType.CREATE_REQUEST, dr)
                        call.respond(uuid.toString())
                    }
                } else {
                    println("${node.id} IS NOT LEADER, WHO THE HELL IS SENDING HTTP REQUESTS")
                }
            }

            get("/read/{id}") {
                val uuid: UUID = UUID.fromString(call.parameters["id"])
                if (node.isLeader()) {
                    val nodes = JumpHash.getDuplicates(uuid.toString(), node.cluster.size + 1)
                    var data: ByteArray? = null
                    println("searching in $nodes")

                    for (n in nodes)
                        try {
                            data = getData(n, uuid)
                        } catch (_: Exception) {
                            println("smth went wrong homie, can't get data from $n")
                        } finally {
                            if (data != null) {
                                call.respondText(String(data, Charset.defaultCharset()))
                                println("got $uuid from $n")
                                break
                            }
                        }
                } else try {
                    call.respondText(String(node.datastore.read(uuid), Charset.defaultCharset()))
                } catch (_: Exception) {
                    println("couldn't get data for id $uuid")
                }
            }

            put("/update/{id}") {
                launch {
                    if (node.isLeader()) {
                        val uuid: UUID = UUID.fromString(call.parameters["id"])
                        val replicaNodes = JumpHash.getDuplicates(uuid.toString(), node.cluster.size + 1)
                        val data: ByteArray = call.receive()
                        val dr = DatastoreRequest(uuid, data)
                        node.executeRequest(replicaNodes, MessageType.UPDATE_REQUEST, dr)
                    } else {
                        println("${node.id} IS NOT LEADER, WHO THE HELL IS SENDING HTTP REQUESTS")
                    }
                }
            }

            delete("/delete/{id}") {
                launch {
                    if (node.isLeader()) {
                        val uuid: UUID = UUID.fromString(call.parameters["id"])
                        val dr = DatastoreRequest(uuid)
                        val replicaNodes = JumpHash.getDuplicates(uuid.toString(), node.cluster.size + 1)
                        node.executeRequest(replicaNodes, MessageType.DELETE_REQUEST, dr)
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

suspend fun getData(nodeId: Int, uuid: UUID): ByteArray {
    return if (nodeId == node.id) {
        node.datastore.read(uuid)
    } else {
        val o = node.cluster[nodeId]!!
        try {
            client.get("http://${o.host}:${o.httpPort}/ds/read/${uuid}").body()
        } catch (e: Exception) {
            throw e
        }
    }
}