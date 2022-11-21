package madalv.log

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import madalv.message.Message
import madalv.message.MessageType
import madalv.node.Node
import madalv.node.Role

class LogManager(val node: Node) {
    val log: MutableList<LogEntry> = mutableListOf()
    var commitLength = 0
    var sentLength = HashMap<Int, Int>()
    var ackedLength = HashMap<Int, Int>()

}