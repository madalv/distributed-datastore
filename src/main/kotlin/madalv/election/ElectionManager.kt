package madalv.election

import io.ktor.network.sockets.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import madalv.log.LogRequest
import madalv.message.*
import madalv.node.Node
import madalv.node.Role
import madalv.protocols.tcp.TCP
import madalv.protocols.udp.UDP
import java.util.*


@OptIn(DelicateCoroutinesApi::class)
class ElectionManager(val node: Node) {
    var currentTerm: Int = 0
    var currentLeader: Int? = null
    var votesReceived: MutableSet<Int> = mutableSetOf()
    val udpClient = UDP.Client
    val tcpClient = TCP.Client
    var votedFor: Int? = null
    val timer: Timer = Timer()
    var electionTimeout: Long? = null
    val voteResponseChannel: Channel<VoteResponse> = Channel(Channel.UNLIMITED)
    
    init {
        GlobalScope.launch {
            collectVotes()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun initElection() {
        votesReceived.clear()
        println("Starting election!")
        currentTerm += 1
        node.currentRole = Role.CANDIDATE
        votesReceived.add(node.id)
        votedFor = node.id
        val lastTerm: Int = if (node.log().size > 0) node.log()[node.log().lastIndex].term else 0

        val request = VoteRequest(currentTerm, node.id, node.log().size, lastTerm)
        val message = Message(MessageType.VOTE_REQUEST, Json.encodeToString(VoteRequest.serializer(), request))

        GlobalScope.launch {
            node.broadcast(Json.encodeToString(Message.serializer(), message))
        }
        startElectionTimer()
    }

    fun vote(vr: VoteRequest) {
        if (vr.term > currentTerm) {
            currentTerm = vr.term
            node.currentRole = Role.FOLLOWER
            votedFor = null
        }

        val lastTerm: Int = if (node.log().size > 0) node.log()[node.log().lastIndex].term else 0
        val logOk = (vr.lastLogTerm > lastTerm) || (vr.lastLogTerm == lastTerm && vr.logLength >= node.log().size)

        if (vr.term == currentTerm && logOk && (votedFor == null || votedFor == vr.candidateId)) {
            votedFor = vr.candidateId
            sendVoteResponse(vr.candidateId, true)
        } else {
            sendVoteResponse(vr.candidateId, false)
        }
    }

    private suspend fun collectVotes() {
        for (vr in voteResponseChannel) {
            if (vr.voteGranted && node.currentRole == Role.CANDIDATE && vr.term == currentTerm) {
                votesReceived.add(vr.nodeId)

                println("Vote received from ${vr.nodeId}. Votes collected: ${votesReceived.size} / ${(node.cluster.size + 2) / 2}")
                if (votesReceived.size >= ((node.cluster.size + 2) / 2)) {
                    node.currentRole = Role.LEADER
                    currentLeader = node.id
                    timer.cancel()
                    println("CONGRATS TO ${node.id} ON BECOMING THE LEADER!")

                    node.cluster.forEach { (_, n) ->
                        node.sentLen()[n.id] = node.log().size
                        node.ackedLen()[n.id] = 0
                    }

                    replicateLog(currentLeader!!)
                }
            } else if (vr.term > currentTerm) {
                currentTerm = vr.term
                node.currentRole = Role.FOLLOWER
                votedFor = null
                timer.cancel()
            }
        }
    }

    private fun sendVoteResponse(candidateId: Int, voteGranted: Boolean) {
        val response = VoteResponse(node.id, currentTerm, voteGranted)
        val message = Message(MessageType.VOTE_RESPONSE, Json.encodeToString(VoteResponse.serializer(), response))
        tcpClient.send(
            InetSocketAddress(node.cluster[candidateId]!!.host, node.cluster[candidateId]!!.tcpPort),
            Json.encodeToString(Message.serializer(), message))
    }

    private fun startElectionTimer() {
        timer.schedule(object: TimerTask() {
            override fun run() {
                initElection()
            }
        }, electionTimeout!!)
    }


    fun receiveLogRequest(lr: LogRequest) {
        if (lr.currentTerm > currentTerm) {
            currentTerm = lr.currentTerm
            votedFor = null
            timer.cancel()
        }
        if (lr.currentTerm == currentTerm) {
            node.currentRole = Role.FOLLOWER
            currentLeader = lr.leaderId
            println("$currentLeader is the NEW LEADER!")
        }

        // TODO add log and ack stuff

    }

    // TODO move this into log manager
    @OptIn(DelicateCoroutinesApi::class)
    fun replicateLog(leaderId: Int) {
        // TODO add actual log stuff
        val lr = LogRequest(leaderId, currentTerm,0, 0, 0, 0)
        val message = Message(MessageType.LOG_REQUEST, Json.encodeToString(LogRequest.serializer(), lr))
        GlobalScope.launch{
            node.broadcast(Json.encodeToString(Message.serializer(), message))
        }
        println("Broadcasted log request to followers!")
    }
}