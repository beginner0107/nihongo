package com.nihongo.conversation.domain.model

data class Hint(
    val japanese: String,
    val korean: String,
    val romaji: String? = null,
    val explanation: String? = null
)

data class HintRequest(
    val context: String,
    val userLevel: Int
)
