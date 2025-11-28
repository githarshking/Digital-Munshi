package com.githarshking.the_digital_munshi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Transaction::class], version = 2, exportSchema = false) // Version bumped to 2
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
                    // This line is CRITICAL for development.
                    // It allows the app to delete the old database if the schema changes,
                    // preventing a crash on startup.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}