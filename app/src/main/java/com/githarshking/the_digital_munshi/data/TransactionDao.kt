package com.githarshking.the_digital_munshi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the Transaction entity.
 * This is where we define our database queries.
 */
@Dao
interface TransactionDao {

    // 'suspend' means this function can be paused and resumed,
    // so it doesn't block the main UI thread.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    // A "Flow" is a modern Kotlin way to observe data changes.
    // Room will automatically update this list whenever the data changes.
    // This is powerful!
    @Query("SELECT * FROM transactions_table ORDER BY transaction_date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    // We'll use this for the report on Day 4
    @Query("SELECT * FROM transactions_table WHERE transaction_date BETWEEN :startDate AND :endDate")
    fun getTransactionsBetweenDates(startDate: Long, endDate: Long): Flow<List<Transaction>>
}