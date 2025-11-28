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
    val type: String,

    @ColumnInfo(name = "transaction_date")
    val date: Long,

    @ColumnInfo(name = "transaction_category")
    val category: String,

    @ColumnInfo(name = "transaction_note")
    val note: String?,

    @ColumnInfo(name = "transaction_source")
    val source: String,

    @ColumnInfo(name = "counterparty")
    val counterparty: String = "Unknown",

    @ColumnInfo(name = "is_verified")
    val isVerified: Boolean = false,

    @ColumnInfo(name = "transaction_hash")
    val transactionHash: String = "",

    // --- NEW AI FIELDS ---
    @ColumnInfo(name = "ai_category")
    val aiCategory: String? = null,

    @ColumnInfo(name = "ai_confidence")
    val aiConfidence: Double = 0.0,

    @ColumnInfo(name = "ai_reasoning")
    val aiReasoning: String? = null
)