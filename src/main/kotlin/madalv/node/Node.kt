package madalv.node

import io.ktor.network.sockets.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import madalv.datastore.Datastore
import madalv.election.ElectionManager
import madalv.log.LogEntry
import madalv.log.LogManager
import madalv.message.Message
import madalv.protocols.tcp.TCP
import madalv.protocols.udp.UDP
import java.util.*
import kotlin.collections.HashMap

// todo jostkii refactoring

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

    @Transient var datastore: Datastore = Datastore()

    @Transient val logManager: LogManager = LogManager(this)


    @Transient val udpClient = UDP.Client
    @Transient val tcpClient = TCP.Client

    @Transient var heartbeatTimer = Timer()
    @Transient var timeoutTimer = Timer()

    @OptIn(DelicateCoroutinesApi::class)
    fun broadcast(message: String) {
        GlobalScope.launch {
            udpClient.broadcast(message)
        }
    }

    /*
    dumb name, but this is the timer that checks if the leader hasn't
    sent a heartbeat in a while (currently 10 sec)
    in the receiveLogRequest() function, it is canceled (if follower received the heartbeat log request)
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
        }, 10100)
    }

    /*
    timer that takes care of the repeated "heartbeat" log replication
    this repeated action lets followers know the leader ain't dead
    */
    fun startHeartbeatTimer() {
        heartbeatTimer = Timer()
        heartbeatTimer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                if (currentRole === Role.LEADER) {
                    println("Heartbeat log replication...")
                    electionManager.replicateLog(id)
                }
            }
        }, electionManager.electionTimeout!! * 2, 5000 )
    }

    fun broadcastMsg(msg: Message) {
        if (currentRole === Role.LEADER) {
            logManager.log.add(LogEntry(msg, electionManager.currentTerm))
            logManager.ackedLength[id] = logManager.log.size
            electionManager.replicateLog(id)
        } else println("Not broadcasting msg - not the leader.")
    }

    //TODO move this to comm manager
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
}