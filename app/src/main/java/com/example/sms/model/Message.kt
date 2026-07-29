package com.example.sms.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderUuid: String,
    val recipientUuid: String,
    val plaintext: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSent: Boolean
)
