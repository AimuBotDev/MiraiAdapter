package com.aimu.aimubot.bot

import com.aimu.aimubot.conf.AccountConf
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.mamoe.mirai.Bot
import java.io.File

class BotWrapper {
    lateinit var bot: Bot
    lateinit var accountConf: AccountConf

    fun loadConfig(): Boolean {
        val configFile = File("AccountConf.json")
        return if (configFile.exists()) {
            accountConf = Json.decodeFromString(configFile.readText())
            true
        } else {
            print("请新建配置AccountConf.json然后重新启动：\n{\"qqId\":qq账号,\"password\":\"qq密码\"}")
            false
        }
    }
}
