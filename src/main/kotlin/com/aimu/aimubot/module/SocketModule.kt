package com.aimu.aimubot.module

import com.aimu.aimubot.bot.botWrapper
import com.aimu.aimubot.bot.messageScheduler
import com.aimu.aimubot.utils.SocketClient
import com.aimu.aimubot.utils.downloadBytes
import com.aimu.aimubot.utils.getSandwichedText
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.GroupTempMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.io.File

@Serializable
public class SocketMessage {
    var type: String = ""
    var conn: String = ""
    var protocol: String = ""
    var botadapter: String = ""
}

class SocketModule : BotModule(), SocketClient.Callback {
    override fun onGetModuleName() = "SocketModule"
    override fun onGetModuleVersion() = "0.0.1"

    private val networkService = SocketClient()

    override fun onInit() {
        networkService.setCallback(this)
    }

    override fun onReload() {
        networkService.connect("127.0.0.1", 10616)
        val m = SocketMessage()
        m.type = "connection"
        m.conn = "socket"
        m.protocol = "cs"
        m.botadapter = "mirai"
        val s = Json.encodeToString(m)
        println(s)
        networkService.sendMessage(s)
    }

    override fun onGroupMessage(event: GroupMessageEvent): Boolean {
        if (!networkService.isConnected)
            onReload()

        val (header, msg) = constructSocketMessage(event)
        networkService.sendMessage(header + msg)

        return false
    }

    override fun onGroupQuotedMessage(id: Int, event: GroupMessageEvent): Boolean {
        if (networkService.isConnected) {
            val (header, msg) = constructSocketMessage(event)
            networkService.sendMessage("$header[cs:quote:$id]$msg")
            val mao = event.message[0]
            if (mao is Image) {
                mao.imageId
            }
        } else
            onReload()

        return false
    }

    data class ConstructMsgRet(val header: String, val body: String)

    private fun constructSocketMessage(event: MessageEvent): ConstructMsgRet {
        val mc = event.message.serializeToMiraiCode()
        when (event) {
            is GroupMessageEvent -> {
                val gl = when (event.sender.permission) {
                    MemberPermission.MEMBER -> 0
                    MemberPermission.ADMINISTRATOR -> 1
                    MemberPermission.OWNER -> 2
                }
                return ConstructMsgRet(
                    "[cs:gid:${event.subject.id}]" +
                            "[cs:gn:${PlainText(event.subject.name).serializeToMiraiCode()}]" +
                            "[cs:id:${event.sender.id}]" +
                            "[cs:n:${PlainText(event.sender.nameCardOrNick).serializeToMiraiCode()}]" +
                            "[cs:mid:${if (event.message.ids.isEmpty()) 0 else event.message.ids[0]}]" +
                            "[cs:gl:${gl}]",
                    mc
                )
            }
            is FriendMessageEvent -> {
                return ConstructMsgRet(
                    "[cs:fid:${event.subject.id}]" +
                            "[cs:fn:${PlainText(event.subject.nameCardOrNick).serializeToMiraiCode()}]" +
                            "[cs:mid:${if (event.message.ids.isEmpty()) 0 else event.message.ids[0]}]",
                    mc
                )
            }
            is GroupTempMessageEvent -> {
                return ConstructMsgRet(
                    "[cs:tid:${event.subject.id}]" +
                            "[cs:tn:${PlainText(event.subject.nameCardOrNick).serializeToMiraiCode()}]" +
                            "[cs:mid:${if (event.message.ids.isEmpty()) 0 else event.message.ids[0]}]",
                    mc
                )
            }
            else -> {
                return ConstructMsgRet("", "")
            }
        }
    }

    private fun handleMessage(msg: String) {
        logMessage(msg)

        // [cs:gs:1222]猫
        // [cs:gs:1111]猫猫猫[cs:image:bot_shared_data/Arknights/tmp_result.jpg]
        // [cs:gs:1111][cs:reply:233]ddtc

        when {
            msg.startsWith("[cs:bot-group-cmd:", true) -> {
                val cmd = msg.getSandwichedText("[cs:gs:", "]")
                val content = msg.substringAfter("[cs:bot-cmd:")
                when (cmd) {
                    "leave-group" -> {
                        val group = botWrapper.bot.getGroup(content.toLong()) ?: return
                        messageScheduler.queueAction { group.quit() }
                    }
                    "mute-member" -> {
                        val params = msg.split(',')
                        val group = botWrapper.bot.getGroup(params[0].toLong()) ?: return
                        val member = group.getMember(params[1].toLong()) ?: return

                        if (group.botPermission.isOperator())
                            messageScheduler.queueAction {
                                member.mute(params[2].toInt())
                            }
                    }
                    "poke-member" -> {
                        val params = msg.split(',')
                        val group = botWrapper.bot.getGroup(params[0].toLong()) ?: return
                        val member = group.getMember(params[1].toLong()) ?: return
                        messageScheduler.queueAction { member.nudge() }
                    }
                }
            }
            msg.startsWith("[cs:gs:") -> {
                val groupId = msg.getSandwichedText("[cs:gs:", "]")
                val replyId = msg.getSandwichedText("[cs:reply:", "]")

                if (groupId.isNotEmpty()) {
                    messageScheduler.groupMessage(groupId.toLong()) { contact ->
                        var mc = MessageChainBuilder()
                        if (replyId.isNotEmpty()) {
                            val tmpMsg = messageScheduler.getMessage(groupId.toLong(), replyId.toInt())
                            if (tmpMsg != null) {
                                mc.append(QuoteReply(tmpMsg.message))
                            }
                        }
                        msg.forEachSocketCode { origin, name, args ->
                            if (name == null) {
                                mc = mc.append(origin)
                            } else if (name.startsWith("cs:")) {
                                val m = name.substringAfter("cs:")

                                if (m == "image") {
                                    if (args.startsWith("http")) {
                                        mc = mc.append(contact.uploadImage(downloadBytes(args).toExternalResource()))
                                    } else
                                        mc = mc.append(contact.uploadImage(File(args)))
                                }

                            } else if (name.isNotEmpty()) {
                                mc = mc.append(origin.deserializeMiraiCode())
                            }
                        }
                        mc.build()
                    }
                }
            }
        }
    }

    private suspend fun String.forEachSocketCode(block: suspend (origin: String, name: String?, args: String) -> Unit) {
        var pos = 0
        var lastPos = 0
        val len = length - 4 // [mirai:
        fun findEnding(start: Int): Int {
            var pos0 = start
            while (pos0 < length) {
                when (get(pos0)) {
                    '\\' -> pos0 += 2
                    ']' -> return pos0
                    else -> pos0++
                }
            }
            return -1
        }
        while (pos < len) {
            when (get(pos)) {
                '\\' -> {
                    pos += 2
                }
                '[' -> {
                    if (get(pos + 1) == 'm' && get(pos + 2) == 'i' &&
                        get(pos + 3) == 'r' && get(pos + 4) == 'a' &&
                        get(pos + 5) == 'i' && get(pos + 6) == ':'
                    ) {
                        val begin = pos
                        pos += 7
                        val ending = findEnding(pos)
                        if (ending == -1) {
                            block(substring(lastPos), null, "")
                            return
                        } else {
                            if (lastPos < begin) {
                                block(substring(lastPos, begin), null, "")
                            }
                            val v = substring(begin, ending + 1)
                            val splitter = v.indexOf(':', 7)
                            block(
                                v, if (splitter == -1)
                                    v.substring(7, v.length - 1)
                                else v.substring(7, splitter),
                                if (splitter == -1) {
                                    ""
                                } else v.substring(splitter + 1, v.length - 1)
                            )
                            lastPos = ending + 1
                            pos = lastPos
                        }
                    } else if (get(pos + 1) == 'c' && get(pos + 2) == 's' && get(pos + 3) == ':'
                    ) {
                        val begin = pos
                        pos += 4
                        val ending = findEnding(pos)
                        if (ending == -1) {
                            block(substring(lastPos), null, "")
                            return
                        } else {
                            if (lastPos < begin) {
                                block(substring(lastPos, begin), null, "")
                            }
                            val v = substring(begin, ending + 1)
                            val splitter = v.indexOf(':', 4)
                            block(
                                v, "cs:" + if (splitter == -1)
                                    v.substring(4, v.length - 1)
                                else v.substring(4, splitter),
                                if (splitter == -1) {
                                    ""
                                } else v.substring(splitter + 1, v.length - 1)
                            )
                            lastPos = ending + 1
                            pos = lastPos
                        }
                    } else pos++
                }
                else -> {
                    pos++
                }
            }
        }
        if (lastPos < length) {
            block(substring(lastPos), null, "")
        }
    }

    override fun onConnected(host: String, port: Int) {
        logMessage("onConnected")
    }

    override fun onConnectFailed(host: String, port: Int) {
        logMessage("onConnectFailed")
    }

    override fun onDisconnected() {
        logMessage("onDisconnected")
        networkService.disconnect()
    }

    override fun onMessageSent(msg: String) {

    }

    override fun onMessageReceived(msg: String) {
        handleMessage(msg)
    }

    override fun onConnectLost() {
        logMessage("onConnectLost")
    }
}
