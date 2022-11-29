package madalv.election

import io.ktor.network.sockets.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import madalv.message.*
import madalv.node.Node
import madalv.node.Role

import java.util.*


@OptIn(DelicateCoroutinesApi::class)
class ElectionManager(val node: Node) {
    var currentTerm: Int = 0
    var currentLeader: Int? = null
    var electionTimeout: Long? = null
    val voteResponseChannel: Channel<VoteResponse> = Channel(Channel.UNLIMITED)
    private var electionIsRunning: Boolean = false
    private var votesReceived: MutableSet<Int> = mutableSetOf()
    private var votedFor: Int? = null
    private var electionTimer: Timer = Timer()
    
    init {
        GlobalScope.launch {
            collectVotes()
        }
    }

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

        node.broadcast(Json.encodeToString(Message.serializer(), message))

        startElectionTimer()
    }

    fun vote(vr: VoteRequest) {
        if (vr.term > currentTerm) resetNodeElectionStatus(vr.term)

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
                    electionTimer.cancel()
                    electionIsRunning = false
                    println("CONGRATS TO ${node.id} ON BECOMING THE LEADER!")

                    node.cluster.forEach { (_, n) ->
                        node.sentLen()[n.id] = node.log().size
                        node.ackedLen()[n.id] = 0
                    }
                    node.replicateLog(currentLeader!!)
                }
            } else if (vr.term > currentTerm) {
                resetNodeElectionStatus(vr.term)
            }
        }
    }

    fun resetNodeElectionStatus(newTerm: Int) {
        currentTerm = newTerm
        node.currentRole = Role.FOLLOWER
        votedFor = null
        if (electionIsRunning) {
            electionTimer.cancel()
            electionIsRunning = false
        }
    }

    private fun sendVoteResponse(candidateId: Int, voteGranted: Boolean) {
        val response = VoteResponse(node.id, currentTerm, voteGranted)
        val message = Message(MessageType.VOTE_RESPONSE, Json.encodeToString(VoteResponse.serializer(), response))
        node.send(
            candidateId,
            Json.encodeToString(Message.serializer(), message))
    }

    private fun startElectionTimer() {
        electionIsRunning = true
        electionTimer = Timer()
        electionTimer.schedule(object: TimerTask() {
            override fun run() {
                initElection()
            }
        }, electionTimeout!!)
    }
}