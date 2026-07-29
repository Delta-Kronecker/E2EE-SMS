package com.example.sms

data class SmsMessage(
    val id: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
    val isSent: Boolean
)
