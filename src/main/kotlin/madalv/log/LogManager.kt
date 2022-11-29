package madalv.log

import kotlinx.serialization.json.Json
import madalv.datastore.DatastoreRequest
import madalv.message.Message
import madalv.message.MessageType
import madalv.node.Node
import madalv.node.Role

class LogManager(private val node: Node) {
    var log: MutableList<LogEntry> = mutableListOf()
    var sentLength = HashMap<Int, Int>()
    var ackedLength = HashMap<Int, Int>()

    fun receiveLogRequest(lr: LogRequest) {
        node.timeoutTimer.cancel()

        if (lr.currentTerm > node.currentTerm()) {
            node.resetNodeElectionStatus(lr.currentTerm)
        }
        if (lr.currentTerm == node.currentTerm()) {
            node.currentRole = Role.FOLLOWER
            node.setCurrentLeader(lr.leaderId)
        }

        if (node.currentTerm() == lr.currentTerm) {
            println("suffix ok for appending")
            appendEntries(lr.prefixLen, lr.commitLen, lr.suffix)
        } else {
            println("suffix not ok for appending: log ok?, term = lr.term? ${node.currentTerm() == lr.currentTerm}")
        }

        node.startTimeoutTimer()
    }

    private fun appendEntries(prefixLen: Int, leaderCommit: Int, suffix: MutableList<LogEntry>) {
        //println("appending entries...")
        if (suffix.size > 0 && log.size > prefixLen) {
            val index = minOf(log.size, prefixLen + suffix.size) - 1
            if (log[index].term != suffix[index - prefixLen].term) {
                log = log.subList(0, prefixLen)
            }
        }

        if (prefixLen + suffix.size > log.size) log = suffix

        for (le in log) {
            if (node.id in le.targetNodes) verifyLogEntry(le)
        }

        println("log size ${log.size}")
    }

    private fun verifyLogEntry(le: LogEntry) {
        val msg = le.msg
        val dr = Json.decodeFromString(DatastoreRequest.serializer(), msg.data)
        when(msg.messageType) {
            MessageType.UPDATE_REQUEST -> {
                if (!node.datastore.read(dr.key!!).contentEquals(dr.data)) node.datastore.update(dr.key, dr.data!!)
            }
            MessageType.CREATE_REQUEST -> {
                if (!node.datastore.containsKey(dr.key!!)) node.datastore.create(dr.key, dr.data!!)
            }
            MessageType.DELETE_REQUEST -> {
                if (node.datastore.containsKey(dr.key!!)) node.datastore.delete(dr.key)
            }
            else -> {
                println("unsupported type for this action - ${le.msg.messageType}")
            }
        }
    }

    fun replicateLog(leaderId: Int) {
        for (n in node.cluster.values) {
            val prefixLen = sentLength[n.id]!!
            val suffix = log.subList(prefixLen, log.size)
            val prefixTerm = if (prefixLen > 0) log[prefixLen.minus(1)].term else 0

            val lr = LogRequest(leaderId, node.currentTerm(),prefixLen, prefixTerm, 0, log)
            val message = Message(MessageType.LOG_REQUEST, Json.encodeToString(LogRequest.serializer(), lr))

            node.send(n.id, Json.encodeToString(Message.serializer(), message))
        }
    }
}