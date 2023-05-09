package com.aimu.aimubot.module

import com.aimu.aimubot.bot.messageScheduler
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.QuoteReply

var moduleMgr: ModuleMgr = ModuleMgr()

class ModuleMgr {
    var modules = mutableListOf<BotModule>()

    fun init() {
        modules.add(SocketModule())

        for (m in modules) {
            try {
                m.onInit()
                m.onReload()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun reloadModule(name: String): String {
        try {
            val m = modules.find { m -> m.onGetModuleName() == name }
            if (m != null)
                m.onReload()
            else
                return ""
        } catch (ex: Exception) {
            return ex.message ?: "unknown error occurred."
        }
        return ""
    }

    fun dispatchGroupQuote(event: GroupMessageEvent): Boolean {
        val quote = event.message[QuoteReply] ?: return false

        if (quote.source.ids.isEmpty())
            return false

        if (messageScheduler.dealQuoteMessage(quote.source.ids[0], event))
            return true

        var rev = false
        for (m in modules) {
            if (m.onGroupQuotedMessage(quote.source.ids[0], event)) {
                rev = true
                break
            }
        }

        return rev
    }

    fun dispatchGroupMessageEvent(event: GroupMessageEvent): Boolean {
        var rev = false

        for (m in modules) {
            if (m.internalDealActions(event)) {
                rev = true
                break
            }
            if (m.onGroupMessage(event)) {
                rev = true
                break
            }
        }

        return rev
    }

    fun stat(): String {
        var s = ""
        for (m in modules) {
            s += "${m.onGetModuleName()}\n"
        }
        return s
    }

    fun onTest(): String {
        return "测试"
    }
}
