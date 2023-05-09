package com.aimu.aimubot.bot

import com.aimu.aimubot.module.SocketMessage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


suspend fun main() {
    val m = SocketMessage()
    m.type = "connection"
    m.conn = "socket"
    m.protocol = "cs"
    m.botadapter = "mirai"
    val s = Json.encodeToString<SocketMessage>(m)
    println(s)
}
