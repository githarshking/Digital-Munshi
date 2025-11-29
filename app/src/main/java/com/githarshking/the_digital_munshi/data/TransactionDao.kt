package com.githarshking.the_digital_munshi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // CHANGED: Returns Long (The Row ID)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions_table WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT * FROM transactions_table ORDER BY transaction_date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions_table WHERE transaction_date BETWEEN :startDate AND :endDate ORDER BY transaction_date DESC")
    fun getTransactionsBetweenDates(startDate: Long, endDate: Long): Flow<List<Transaction>>

    // ... other methods ...
    // Checks if a similar transaction already exists (to prevent duplicates)
    @Query("SELECT COUNT(*) FROM transactions_table WHERE transaction_amount = :amount AND transaction_date = :date AND transaction_type = :type")
    suspend fun checkDuplicate(amount: Double, date: Long, type: String): Int
}