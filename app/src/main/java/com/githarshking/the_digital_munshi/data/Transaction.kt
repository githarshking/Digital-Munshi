package com.githarshking.the_digital_munshi.data // Make sure this is your package name

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * This is our main data model, NOW annotated as a Room Entity.
 * This tells Room to create a database table named "transactions_table"
 * based on this class.
 */
@Entity(tableName = "transactions_table")
data class Transaction(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L, // Room will auto-generate a unique ID for each entry

    @ColumnInfo(name = "transaction_amount")
    val amount: Double,

    @ColumnInfo(name = "transaction_type")
    val type: String,     // "INCOME" or "EXPENSE"

    @ColumnInfo(name = "transaction_date")
    val date: Long,       // Unix Timestamp

    @ColumnInfo(name = "transaction_category")
    val category: String, // "Groceries", "Salary", etc.

    @ColumnInfo(name = "transaction_note")
    val note: String?,    // Optional note

    @ColumnInfo(name = "transaction_source")
    val source: String    // "MANUAL", "SMS"
)