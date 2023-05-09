package com.aimu.aimubot.bot

import com.aimu.aimubot.module.moduleMgr
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.message.sourceIds
import net.mamoe.mirai.utils.BotConfiguration
import java.time.LocalTime
import kotlin.random.Random

var botWrapper = BotWrapper()

suspend fun main() {
    Random(LocalTime.now().toNanoOfDay())

    botWrapper.loadConfig()

    println("Init Modules")
    moduleMgr.init()

    println("Init bot")
    botWrapper.bot = BotFactory.newBot(botWrapper.accountConf.qqId, botWrapper.accountConf.password) {
        fileBasedDeviceInfo("device.json")
        protocol = BotConfiguration.MiraiProtocol.ANDROID_PAD
    }.alsoLogin()

    botWrapper.bot.eventChannel.subscribeAlways<MessageEvent> { event -> onMessageEvent(event) }
    botWrapper.bot.eventChannel.subscribeAlways<MessageRecallEvent> { event -> onGroupRecallEvent(event) }

    botWrapper.bot.eventChannel.subscribeAlways<BotInvitedJoinGroupRequestEvent> { event -> onBotEvent(event) }
    botWrapper.bot.eventChannel.subscribeAlways<BotJoinGroupEvent> { event -> onBotEvent(event) }
    botWrapper.bot.eventChannel.subscribeAlways<BotLeaveEvent.Kick> { event -> onBotEvent(event) }
    botWrapper.bot.eventChannel.subscribeAlways<BotLeaveEvent.Active> { event -> onBotEvent(event) }
    botWrapper.bot.eventChannel.subscribeAlways<BotMuteEvent> { event -> onBotEvent(event) }
    botWrapper.bot.eventChannel.subscribeAlways<BotUnmuteEvent> { event -> onBotEvent(event) }

    messageScheduler.startActionDealer()

    botWrapper.bot.join()
}

suspend fun onBotEvent(event: BotEvent) {
    when (event) {
        is BotLeaveEvent.Kick -> {
            event.operator.id
        }
        is BotLeaveEvent.Active -> {

        }
        is BotMuteEvent -> {

        }
        is BotUnmuteEvent -> {

        }
        is BotJoinGroupEvent -> {

        }
        is BotInvitedJoinGroupRequestEvent->{

        }
    }
}

suspend fun onBotInvitedJoinGroupRequestEvent(event: BotInvitedJoinGroupRequestEvent) {

}

suspend fun onBotJoinGroupEvent(event: BotJoinGroupEvent) {
    println("BotJoinedGroup: " + event.groupId)
}

suspend fun onGroupRecallEvent(event: MessageRecallEvent) {
    //moduleMgr.dispatchGroupRecallEvent(event)
}

suspend fun onGroupMessageEvent(event: GroupMessageEvent) {
    if (event.message[QuoteReply] != null) {
        moduleMgr.dispatchGroupQuote(event)
    } else if (event.message.content.startsWith("[mirai:service", 0)) {

    } else {
        moduleMgr.dispatchGroupMessageEvent(event)
    }
}

suspend fun onMessageEvent(event: MessageEvent) {
    when (event) {
        is GroupMessageEvent -> {
            messageScheduler.pushMessage(event)
            onGroupMessageEvent(event)
        }
        is FriendMessageEvent -> {
            messageScheduler.pushMessage(event)
        }
        is GroupTempMessageEvent -> {
            messageScheduler.pushMessage(event)
        }
        else -> {}
    }
}
