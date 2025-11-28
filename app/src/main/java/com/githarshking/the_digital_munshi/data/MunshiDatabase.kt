package com.githarshking.the_digital_munshi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// UPDATED: Version is now 3
@Database(entities = [Transaction::class], version = 3, exportSchema = false)
abstract class MunshiDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: MunshiDatabase? = null

        fun getDatabase(context: Context): MunshiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MunshiDatabase::class.java,
                    "munshi_database"
                )
                    // This deletes the old DB if the version changes (Data loss is expected in Dev)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}