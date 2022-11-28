package madalv.log

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import madalv.message.Message
import madalv.message.MessageType
import madalv.node.Node
import madalv.node.Role
import madalv.protocols.udp.UDP

class LogManager(val node: Node) {
    val log: MutableList<LogEntry> = mutableListOf()
    var commitLength = 0
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

        // TODO add log and ack stuff
        node.startTimeoutTimer()
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun replicateLog(leaderId: Int) {
        // TODO add actual log stuff
        val lr = LogRequest(leaderId, node.currentTerm(),0, 0, 0, 0)
        val message = Message(MessageType.LOG_REQUEST, Json.encodeToString(LogRequest.serializer(), lr))
        GlobalScope.launch {
            UDP.Client.broadcast(Json.encodeToString(Message.serializer(), message))
        }
    }
}