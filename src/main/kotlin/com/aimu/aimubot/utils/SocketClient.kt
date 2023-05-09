package com.aimu.aimubot.utils

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket

class SocketClient() {
    interface Callback {
        fun onConnected(host: String, port: Int) //连接成功
        fun onConnectFailed(host: String, port: Int) //连接失败
        fun onDisconnected() //已经断开连接
        fun onMessageSent(msg: String) //消息已经发出
        fun onMessageReceived(msg: String) //收到消息
        fun onConnectLost()
    }

    private var callback: Callback? = null
    fun setCallback(callback: Callback?) {
        this.callback = callback
    }

    var socket: Socket? = null
    var isConnected = false

    private var inputStream: DataInputStream? = null
    private var outputStream: DataOutputStream? = null

    var hasHeader = false
    var msgLength = 0
    var receivedLength = 0
    var msgBody: ByteArray? = null

    fun connect(host: String, port: Int) {
        try {
            socket = Socket(host, port)
            isConnected = true
            callback?.onConnected(host, port)

            hasHeader = false
            msgLength = 0
            receivedLength = 0

            beginListening()
        } catch (e: IOException) {
            isConnected = false
            callback?.onConnectFailed(host, port)
            //e.printStackTrace()
            println(e.message)
        }
    }

    private fun beginListening() {
        val listening = kotlinx.coroutines.Runnable {
            try {
                inputStream = DataInputStream(socket!!.getInputStream())
                while (true) {
                    if (!hasHeader) {
                        msgLength = inputStream!!.readInt()
                        hasHeader = true
                        receivedLength = 0
                        msgBody = ByteArray(msgLength)
                    } else {
                        val c = inputStream!!.readByte()
                        msgBody?.set(receivedLength, c)
                        receivedLength++
                        if (receivedLength >= msgLength) {
                            if (msgBody != null) {
                                val s = String(msgBody!!, Charsets.UTF_8)
                                callback?.onMessageReceived(s)
                            }
                            hasHeader = false
                            receivedLength = 0
                        }
                    }
                }
            } catch (e: IOException) {
                isConnected = false
                callback?.onConnectLost()
                //e.printStackTrace()
                println(e.message)
            }
        }
        Thread(listening).start()
    }

    fun disconnect() {
        try {
            if (socket != null) {
                socket!!.close()
            }
            if (inputStream != null) {
                inputStream!!.close()
            }
            outputStream?.close()
            callback?.onDisconnected()

        } catch (e: IOException) {
            //e.printStackTrace()
            println(e.message)
        } finally {
            isConnected = false
        }
    }

    fun sendMessage(msg: String) {
        if (msg.isEmpty())
            return

        if (socket == null)
            return

        try {
            outputStream = DataOutputStream(socket!!.getOutputStream())
            val buf = msg.toByteArray(charset = Charsets.UTF_8)
            outputStream!!.writeInt(buf.size)
            outputStream!!.write(buf)
            outputStream!!.flush()
            callback?.onMessageSent(msg)
        } catch (e: IOException) {
            isConnected = false
            callback?.onConnectLost()
            //e.printStackTrace()
            println(e.message)
        }
    }
}
