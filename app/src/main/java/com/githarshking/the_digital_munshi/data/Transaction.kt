package com.githarshking.the_digital_munshi.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions_table")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "transaction_amount")
    val amount: Double,

    @ColumnInfo(name = "transaction_type")
    val type: String,     // "INCOME" or "EXPENSE"

    @ColumnInfo(name = "transaction_date")
    val date: Long,

    @ColumnInfo(name = "transaction_category")
    val category: String,

    @ColumnInfo(name = "transaction_note")
    val note: String?,

    @ColumnInfo(name = "transaction_source")
    val source: String,    // "MANUAL", "SMS"


    @ColumnInfo(name = "counterparty")
    val counterparty: String = "Unknown", // Who paid you? (e.g., "Zomato", "Client Name")

    @ColumnInfo(name = "is_verified")
    val isVerified: Boolean = false,      // True if from SMS, False if Manual

    @ColumnInfo(name = "transaction_hash")
    val transactionHash: String = ""      // Unique ID to prevent duplicate SMS
)