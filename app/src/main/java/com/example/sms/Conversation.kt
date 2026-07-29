package com.example.sms

data class Conversation(
    val address: String,
    val lastMessage: String,
    val lastTimestamp: Long,
    val messageCount: Int
)
