package com.githarshking.the_digital_munshi.data


data class Transaction (
    val id: Long = 0L, // 0L means it's a Long number. This will be the unique key.
    val amount: Double,
    val type: String,     // "INCOME" or "EXPENSE"
    val date: Long,       // We'll store the date as a "Unix Timestamp" (a single long number)
    val category: String, // "Groceries", "Salary", "Fuel", etc.
    val note: String?,    // The '?' means this can be null (optional)
    val source: String    // "MANUAL", "SMS", etc.

)
