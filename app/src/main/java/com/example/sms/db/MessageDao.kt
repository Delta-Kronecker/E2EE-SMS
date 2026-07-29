package com.example.sms.db

import androidx.room.*
import com.example.sms.model.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE senderUuid = :contactUuid OR recipientUuid = :contactUuid ORDER BY timestamp ASC")
    fun getMessagesForContact(contactUuid: String): Flow<List<Message>>

    @Insert
    suspend fun insertMessage(message: Message)

    @Query("DELETE FROM messages WHERE senderUuid = :contactUuid OR recipientUuid = :contactUuid")
    suspend fun deleteMessagesForContact(contactUuid: String)
}
