package com.aimu.aimubot.bot

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.ids
import net.mamoe.mirai.message.sourceIds
import java.io.File
import kotlin.random.Random

var messageScheduler: MessageScheduler = MessageScheduler()

data class NeedReplyMessageDesc(val id: Int, val timeout: Int)

class MessageScheduler {
    val messageCache = mutableListOf<MessageEvent>()

    fun pushMessage(msg: MessageEvent) {
        messageCache.add(msg)
        if (messageCache.size > 4096)
            messageCache.removeFirst()
    }

    fun getMessage(id: Long, mid: Int): MessageEvent? {
        return messageCache.find { it.subject.id == id && it.message.ids[0] == mid }
    }

    var actions = mutableListOf<suspend () -> Unit>()
    var messageActions = mutableListOf<Pair<MessageEvent, suspend () -> Unit>>()
    var contactActions = mutableListOf<Pair<Contact, suspend () -> Unit>>()
    var replyListeners = mutableListOf<Pair<NeedReplyMessageDesc, (MessageEvent) -> Boolean>>()

    private var delayms: Long = 1000

    fun dealQuoteMessage(id: Int, event: MessageEvent): Boolean {
        if (replyListeners.isNotEmpty()) {
            val listenerDesc = replyListeners.find { x -> x.first.id == id }

            if (listenerDesc != null) {
                var needDelete = false

                needDelete = try {
                    listenerDesc.second.invoke(event)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    true
                }

                if (needDelete)
                    replyListeners.remove(listenerDesc)
            }
        }
        return false
    }

    /**
     * 处理消息事件队列
     */
    private suspend fun dealMessageAction(): Boolean {
        if (messageActions.isNotEmpty()) {
            try {
                messageActions.first().second.invoke()
                return true
            } catch (ex: Exception) {
                try {
                    val event = messageActions.first().first
                    val chain: MessageChain = MessageChainBuilder()
                        .append(QuoteReply(event.message))
                        .append(ex.toString())
                        .build()
                    event.subject.sendMessage(chain)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                ex.printStackTrace()
                return false
            } finally {
                messageActions.removeFirst()
            }
        }
        return false
    }

    private suspend fun dealContactAction(): Boolean {
        if (contactActions.isNotEmpty()) {
            try {
                contactActions.first().second.invoke()
                return true
            } catch (ex: Exception) {
                try {
                    val contact = contactActions.first().first
                    val chain: MessageChain = MessageChainBuilder()
                        .append("内部错误:")
                        .append("System.NullReferenceException")
                        //.append(ex.toString().substringAfter("Exception:").trim())
                        .build()
                    contact.sendMessage(chain)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                ex.printStackTrace()
                return false
            } finally {
                contactActions.removeFirst()
            }
        }
        val x=0;val y=0;
        return false
    }

    private suspend fun dealAction(): Boolean {
        if (actions.isNotEmpty()) {
            return try {
                actions.first().invoke()
                true
            } catch (ex: Exception) {
                ex.printStackTrace()
                false
            } finally {
                actions.removeFirst()
            }
        }
        return false
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun startActionDealer() {
        GlobalScope.launch {
            while (true) {
                delayms = Random.nextLong(1500, 4500)
                delay(delayms)
                val result = dealMessageAction()
                if (result) delay(delayms) else delay(1)
                dealContactAction()
                dealAction()
            }
        }
    }

    fun queueAction(onExec : suspend () -> Unit) {
        actions.add(onExec)
    }

    fun replyListenerMessage(
        event: MessageEvent,
        timeout: Int,
        onBuildMsg: suspend () -> MessageChain,
        onReply: (event: MessageEvent) -> Boolean
    ) {
        messageActions.add(Pair(event) {
            val receipt = event.subject.sendMessage(onBuildMsg())
            replyListeners.add(Pair(NeedReplyMessageDesc(receipt.sourceIds[0], timeout), onReply))
        })
    }

    fun receiptAwareMessage(
        event: MessageEvent,
        chain: MessageChain,
        onMsgSent: suspend (receipt: MessageReceipt<Contact>) -> Unit
    ) {
        messageActions.add(Pair(event) {
            val receipt = event.subject.sendMessage(chain)
            onMsgSent(receipt)
        })
    }

    fun receiptAwareMessageBuilder(
        event: MessageEvent,
        onBuildMsg: suspend () -> MessageChain,
        onMsgSent: suspend (receipt: MessageReceipt<Contact>) -> Unit
    ) {
        messageActions.add(Pair(event) {
            val receipt = event.subject.sendMessage(onBuildMsg())
            onMsgSent(receipt)
        })
    }

    fun groupMessage(id: Long, onBuildMsg: suspend (Contact) -> MessageChain) {
        val group: Contact? = botWrapper.bot.getGroup(id)
        if (group != null)
            contactActions.add(Pair(group) { group.sendMessage(onBuildMsg(group)) })
    }

    fun simpleGroupMessage(id: Long, content: String) {
        if (content.isEmpty())
            return
        val group: Contact? = botWrapper.bot.getGroup(id)
        if (group != null)
            contactActions.add(Pair(group) { group.sendMessage(content) })
    }

    fun message(event: MessageEvent, onBuildMsg: suspend () -> MessageChain) {
        messageActions.add(Pair(event) { event.subject.sendMessage(onBuildMsg()) })
    }

    fun simpleMessage(event: MessageEvent, content: String) {
        if (content.isEmpty())
            return
        messageActions.add(Pair(event) { event.subject.sendMessage(content) })
    }

    fun imageMessage(event: MessageEvent, imagePath: String) {
        if (imagePath.isEmpty())
            return
        messageActions.add(Pair(event) { event.subject.sendImage(File(imagePath)) })
    }

    fun reply(event: MessageEvent, onBuildMsg: suspend () -> MessageChain) {
        val c = QuoteReply(event.message)
        messageActions.add(Pair(event) { event.subject.sendMessage(c + onBuildMsg()) })
    }

    fun simpleReply(event: MessageEvent, content: String) {
        if (content.isEmpty())
            return
        val chain: MessageChain = MessageChainBuilder()
            .append(QuoteReply(event.message))
            .append(content)
            .build()
        messageActions.add(Pair(event) { event.subject.sendMessage(chain) })
    }

    fun imageReply(event: MessageEvent, imagePath: String) {
        if (imagePath.isEmpty())
            return
        messageActions.add(Pair(event) {
            val chain: MessageChain = MessageChainBuilder()
                .append(QuoteReply(event.message))
                .append(event.subject.uploadImage(File(imagePath)))
                .build()
            event.subject.sendMessage(chain)
        })
    }

}
