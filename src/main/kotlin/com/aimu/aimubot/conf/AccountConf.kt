package com.aimu.aimubot.conf

import kotlinx.serialization.Serializable

@Serializable
data class AccountConf(
    val qqId: Long,
    val password: String
)
