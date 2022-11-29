package madalv.protocols.http

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import madalv.datastore.DatastoreRequest
import madalv.datastore.JumpHash
import madalv.message.MessageType
import madalv.node
import java.nio.charset.Charset
import java.util.*

suspend fun handleCreate(call: ApplicationCall) {
    val data: ByteArray = call.receive()
    val uuid = UUID.randomUUID()
    val replicaNodes = JumpHash.getDuplicates(uuid.toString(), node.cluster.size + 1)
    println("Creating copies of data in nodes $replicaNodes: $uuid")
    val dr = DatastoreRequest(uuid, data)
    node.executeRequest(replicaNodes, MessageType.CREATE_REQUEST, dr)
    call.respond(uuid.toString())
}

suspend fun handleUpdate(call: ApplicationCall) {
    val uuid: UUID = UUID.fromString(call.parameters["id"])
    val replicaNodes = JumpHash.getDuplicates(uuid.toString(), node.cluster.size + 1)
    val data: ByteArray = call.receive()
    val dr = DatastoreRequest(uuid, data)
    node.executeRequest(replicaNodes, MessageType.UPDATE_REQUEST, dr)
}

suspend fun handleGet(call: ApplicationCall) {
    val uuid: UUID = UUID.fromString(call.parameters["id"])
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
}

fun handleDelete(call: ApplicationCall) {
    val uuid: UUID = UUID.fromString(call.parameters["id"])
    val dr = DatastoreRequest(uuid)
    val replicaNodes = JumpHash.getDuplicates(uuid.toString(), node.cluster.size + 1)
    node.executeRequest(replicaNodes, MessageType.DELETE_REQUEST, dr)
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

suspend fun redirectCreate(call: ApplicationCall) {
    val data: ByteArray = call.receive()
    val nodeId = (node.cluster.keys).random()
    val o = node.cluster[nodeId]!!
    client.post("http://${o.host}:${o.httpPort}/ds/create") {
        setBody(data)
        headers {
            append("Leader-Redirect", "true")
        }
    }
    println("REDIRECTED CREATE TO $nodeId")
}