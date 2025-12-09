package edu.iu.luddy.capstone

import java.io.Serializable

data class Transaction(
    val id: Long,
    val amount: Double,
    val category: String,
    val date: String,
    val description: String,
    val type: TransactionType,
    val currency: String = "USD",
    val isTaxDeductible: Boolean = false,
    val isRecurring: Boolean = false
) : Serializable

enum class TransactionType {
    INCOME,
    EXPENSE
}

object TransactionCategories {
    val EXPENSE_CATEGORIES = listOf(
        "Food & Dining",
        "Transportation",
        "Shopping",
        "Entertainment",
        "Bills & Utilities",
        "Healthcare",
        "Education",
        "Other"
    )

    val INCOME_CATEGORIES = listOf(
        "Salary",
        "Freelance",
        "Investment",
        "Gift",
        "Other"
    )
}