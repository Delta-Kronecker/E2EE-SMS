package com.example.sms.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.sms.model.Contact
import com.example.sms.model.Message

@Database(entities = [Contact::class, Message::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "e2ee_sms_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
