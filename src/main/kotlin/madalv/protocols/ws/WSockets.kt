package madalv.protocols.ws
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.Json
import madalv.message.Message
import madalv.node
import java.time.Duration

val updateChannel = Channel<Message>(Channel.UNLIMITED)
fun Application.configureSockets() {

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/ws") {
            // websocketSession
            if (node.isLeader())
                for (msg in updateChannel) {
                    outgoing.send(Frame.Text(Json.encodeToString(Message.serializer(), msg)))
                }
            }
        }
    }


