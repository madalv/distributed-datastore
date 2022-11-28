package madalv.node

import io.ktor.network.sockets.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import madalv.datastore.Datastore
import madalv.datastore.DatastoreRequest
import madalv.election.ElectionManager
import madalv.log.LogEntry
import madalv.log.LogManager
import madalv.log.LogRequest
import madalv.message.Message
import madalv.message.MessageType
import madalv.protocols.tcp.TCP
import madalv.protocols.udp.UDP
import java.util.*

@Serializable
class Node(
    @SerialName("id") val id: Int,
    @SerialName("http_port") val httpPort: Int,
    @SerialName("udp_port") val udpPort: Int,
    @SerialName("tcp_port") val tcpPort: Int,
    @SerialName("host") val host: String
    ) {
    // other node in the cluster except the current node itself
    @Transient var cluster = HashMap<Int, Node>()
    // deals with leader elections, voting, collecting votes
    @Transient var electionManager: ElectionManager = ElectionManager(this)
    // current role, according to the raft algorithm
    @Transient var currentRole: Role = Role.FOLLOWER
    // datastore that actually keeps all data
    @Transient var datastore: Datastore = Datastore()
    // deals with all Raft log stuff (replication, etc, etc)
    @Transient val logManager: LogManager = LogManager(this)
    // socket clients responsible for sending stuff to other servers
    @Transient val udpClient = UDP.Client
    @Transient val tcpClient = TCP.Client
    // timers, explained below
    @Transient var heartbeatTimer = Timer()
    @Transient var timeoutTimer = Timer()

    @Transient val timeoutPeriod: Long = 10100
    @Transient val heartbeatInterval: Long = 5000

    /**
    dumb name, but this is the timer that checks if the leader hasn't
    sent a heartbeat in a while (currently 10 sec)
    in the receiveLogRequest() function, it is canceled (if follower received the heartbeat log request).
    if 10 sec pass by and it isn't canceled, node considers leader dead and inits an election
    */
    fun startTimeoutTimer() {
        timeoutTimer = Timer()
        timeoutTimer.schedule(object: TimerTask() {
            override fun run() {
                if (currentRole != Role.LEADER) {
                    println("No heartbeat in 10+ sec - starting election...")
                    electionManager.initElection()
                }
            }
        }, timeoutPeriod + Random(System.currentTimeMillis()).nextInt(1000))
    }

    /**
    timer that takes care of the repeated "heartbeat" log replication.
    this repeated action lets followers know the leader ain't dead
    */
    fun startHeartbeatTimer() {
        heartbeatTimer = Timer()
        heartbeatTimer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                if (currentRole === Role.LEADER) {
                    println("Heartbeat log replication...")
                    logManager.replicateLog(id)
                }
            }
        }, electionManager.electionTimeout!! * 2, heartbeatInterval)
    }

    fun executeRequest(nodes: Set<Int>, type: MessageType, dr: DatastoreRequest) {
        val message = Message(type, Json.encodeToString(DatastoreRequest.serializer(), dr))
        val le = LogEntry(message, currentTerm(), nodes, id)
        logManager.ackedLength[id]  = log().size
        appendLogEntry(le)

        for (nodeId in nodes) {
            if (nodeId == id) when (type) {
                MessageType.UPDATE_REQUEST -> datastore.update(dr.key!!, dr.data!!)
                MessageType.CREATE_REQUEST -> datastore.create(dr.key!!, dr.data!!)
                MessageType.DELETE_REQUEST -> datastore.delete(dr.key!!)
                else -> println("Bruh")
            } else {
                send(nodeId, Json.encodeToString(Message.serializer(), message))
            }
        }
    }

    fun appendLogEntry(le: LogEntry) {
        println("appending $le to log, size ${log().size}")
        logManager.log.add(le)
    }

    fun send(nodeId: Int, message: String) {
        tcpClient.send(InetSocketAddress(cluster[nodeId]!!.host, cluster[nodeId]!!.tcpPort), message)
    }

    fun setCluster(nodes: Map<Int, Node>) {
        cluster = nodes.filter { m -> m.key != id } as HashMap<Int, Node>
    }
    override fun toString(): String {
        return "{id=$id http=$httpPort, udp=$udpPort, tcp=$tcpPort, host=$host}"
    }

    fun leaderExists(): Boolean {
        return electionManager.currentLeader != null
    }

    fun log() : MutableList<LogEntry> {
        return logManager.log
    }

    fun ackedLen(): HashMap<Int, Int> {
        return logManager.ackedLength
    }

    fun sentLen(): HashMap<Int, Int> {
        return logManager.sentLength
    }

    fun isLeader(): Boolean {
        return currentRole == Role.LEADER
    }

    fun replicateLog(leaderId: Int) {
        logManager.replicateLog(leaderId)
    }

    fun currentTerm(): Int {
        return electionManager.currentTerm
    }

    fun setCurrentLeader(leaderId: Int){
        electionManager.currentLeader = leaderId
    }

    fun resetNodeElectionStatus(newTerm: Int) {
        electionManager.resetNodeElectionStatus(newTerm)
    }

    fun receiveLogRequest(lr: LogRequest) {
        logManager.receiveLogRequest(lr)
    }
}