package com.aimu.aimubot.module

import com.aimu.aimubot.bot.messageScheduler
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.content

data class SimpleInstructionAction(
    val inst: String,
    val action: (event: GroupMessageEvent, msgBody: String) -> Boolean
)

data class CheckKeywordResult(
    val result: Boolean,
    val msgBody: String
)

open class BotModule {
    private val simpleInstructionActions = mutableListOf<SimpleInstructionAction>()

    open fun onGetModuleName(): String = "ModuleBase"
    open fun onGetModuleVersion(): String = "0.0.1"

    open fun onInit() = onReload()
    open fun onReload() {}

    open fun onHelp(event: MessageEvent): Boolean = false

    open fun onFriendMessageEvent(event: FriendMessageEvent): Boolean = false

    open fun onGroupMessage(event: GroupMessageEvent): Boolean = false
    open fun onGroupRecallEvent(event: MessageRecallEvent): Boolean = false

    open fun onGroupTempMessageEvent(event: GroupTempMessageEvent): Boolean = false

    open fun onBotInvitedJoinGroupRequestEvent(event: BotInvitedJoinGroupRequestEvent): Boolean = false
    open fun onBotJoinGroupEvent(event: BotJoinGroupEvent): Boolean = false

    open fun onGroupQuotedMessage(id: Int, event: GroupMessageEvent): Boolean = false

    fun logMessage(message: Any?) {
        print("[${onGetModuleName()}] ")
        println(message)
    }

    fun registerSimpleInstGroupMessageHandler(
        inst: String,
        action: (event: GroupMessageEvent, msgBody: String) -> Boolean
    ) {
        simpleInstructionActions.add(SimpleInstructionAction(inst, action))
    }

    fun internalDealActions(event: GroupMessageEvent): Boolean {
        simpleInstructionActions.forEach {
            val (inst, action) = it

            val (succeed, msgBody) = checkKeyword(inst, event)

            if (succeed) {
                return action.invoke(event, msgBody)

            }
        }
        return false
    }

    /**
     * 检查是否满足 [keyword] 关键词，并返回去除 [keyword] 的结果，否则返回空字符串
     */
    fun checkKeyword(keyword: String, event: MessageEvent): CheckKeywordResult {
        val s = event.message.content.trim()
        if (keyword == "/") {
            val wd = "/"
            if (s.startsWith(wd, true)) {
                return CheckKeywordResult(true, s.drop(wd.length).trim())
            }
        } else {
            val wd: String = "/$keyword"
            if (s.startsWith(wd, true)) {
                return CheckKeywordResult(true, s.drop(wd.length).trim())
            }
        }
        return CheckKeywordResult(false, "")
    }
}
