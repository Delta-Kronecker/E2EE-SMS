package com.example.sms.db

import androidx.room.*
import com.example.sms.model.Contact
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY pairedAt DESC")
    fun getAllContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE uuid = :uuid")
    suspend fun getContactByUuid(uuid: String): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)

    @Delete
    suspend fun deleteContact(contact: Contact)

    @Query("UPDATE contacts SET phoneNumber = :phone WHERE uuid = :uuid")
    suspend fun updatePhoneNumber(uuid: String, phone: String)
}
