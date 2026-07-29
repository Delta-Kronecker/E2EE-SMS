package com.example.sms.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey
    val uuid: String,
    val name: String,
    val publicKey: String,
    val phoneNumber: String = "",
    val pairedAt: Long = System.currentTimeMillis()
)
