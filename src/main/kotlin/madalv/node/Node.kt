package madalv.node

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import madalv.datastore.Datastore
import madalv.election.ElectionManager

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
    //
    @Transient var datastore: Datastore = Datastore()

    fun setCluster(nodes: Map<Int, Node>) {
        cluster = nodes.filter { m -> m.key != id } as HashMap<Int, Node>
    }
    override fun toString(): String {
        return "{id=$id http=$httpPort, udp=$udpPort, tcp=$tcpPort, host=$host}"
    }

    fun leaderExists(): Boolean {
        return electionManager.currentLeader != null
    }

    fun isLeader(): Boolean {
        return currentRole == Role.LEADER
    }

}