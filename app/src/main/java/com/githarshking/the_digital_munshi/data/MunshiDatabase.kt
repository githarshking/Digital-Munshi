package com.githarshking.the_digital_munshi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The main Room Database class for the app.
 * It lists all the 'entities' (tables) and the 'version' number.
 */
@Database(entities = [Transaction::class], version = 1, exportSchema = false)
abstract class MunshiDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    // This 'companion object' makes the database a "Singleton".
    // This ensures that only ONE instance of the database exists
    // in the entire app, which is crucial for performance.
    companion object {
        @Volatile
        private var INSTANCE: MunshiDatabase? = null

        fun getDatabase(context: Context): MunshiDatabase {
            // Return the existing instance if it's there
            // Otherwise, create a new one
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MunshiDatabase::class.java,
                    "munshi_database" // This is the file name of the database on the phone
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}