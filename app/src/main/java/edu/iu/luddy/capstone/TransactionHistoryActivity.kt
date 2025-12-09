package edu.iu.luddy.capstone

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import com.google.android.material.textview.MaterialTextView

class TransactionHistoryActivity : AppCompatActivity() {

    private lateinit var transactionsRecyclerView: RecyclerView
    private lateinit var emptyStateText: MaterialTextView
    private lateinit var transactionAdapter: TransactionAdapter
    private var transactions = mutableListOf<Transaction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Transaction History"

        initializeViews()
        loadTransactions()
        setupRecyclerView()
        updateEmptyState()
    }

    private fun initializeViews() {
        transactionsRecyclerView = findViewById(R.id.transactionsRecyclerView)
        emptyStateText = findViewById(R.id.emptyStateText)
    }

    private fun loadTransactions() {
        val sharedPreferences = getSharedPreferences("QuantumLedger", MODE_PRIVATE)
        val count = sharedPreferences.getInt("transaction_count", 0)

        transactions.clear()

        for (i in 0 until count) {
            val id = sharedPreferences.getLong("transaction_${i}_id", 0L)
            val amount = sharedPreferences.getString("transaction_${i}_amount", "0.0")?.toDoubleOrNull() ?: 0.0
            val category = sharedPreferences.getString("transaction_${i}_category", "Other") ?: "Other"
            val date = sharedPreferences.getString("transaction_${i}_date", "") ?: ""
            val description = sharedPreferences.getString("transaction_${i}_description", "") ?: ""
            val type = sharedPreferences.getString("transaction_${i}_type", "EXPENSE") ?: "EXPENSE"
            val currency = sharedPreferences.getString("transaction_${i}_currency", "USD") ?: "USD"
            val isTaxDeductible = sharedPreferences.getBoolean("transaction_${i}_tax", false)
            val isRecurring = sharedPreferences.getBoolean("transaction_${i}_recurring", false)

            if (id > 0) {
                transactions.add(
                    Transaction(
                        id = id,
                        amount = amount,
                        category = category,
                        date = date,
                        description = description,
                        type = TransactionType.valueOf(type),
                        currency = currency,
                        isTaxDeductible = isTaxDeductible,
                        isRecurring = isRecurring
                    )
                )
            }
        }

        transactions.sortByDescending { it.id }
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            transactions,
            onEdit = { transaction -> editTransaction(transaction) },
            onDelete = { transaction -> deleteTransaction(transaction) }
        )

        transactionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@TransactionHistoryActivity)
            adapter = transactionAdapter
        }
    }

    private fun updateEmptyState() {
        if (transactions.isEmpty()) {
            transactionsRecyclerView.visibility = android.view.View.GONE
            emptyStateText.visibility = android.view.View.VISIBLE
        } else {
            transactionsRecyclerView.visibility = android.view.View.VISIBLE
            emptyStateText.visibility = android.view.View.GONE
        }
    }

    private fun editTransaction(transaction: Transaction) {
        val intent = Intent(this, AddTransactionActivity::class.java)
        intent.putExtra("transaction_id", transaction.id)
        intent.putExtra("transaction_amount", transaction.amount)
        intent.putExtra("transaction_category", transaction.category)
        intent.putExtra("transaction_date", transaction.date)
        intent.putExtra("transaction_description", transaction.description)
        intent.putExtra("transaction_type", transaction.type.name)
        startActivity(intent)
    }

    private fun deleteTransaction(transaction: Transaction) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                transactions.remove(transaction)
                saveTransactions()
                transactionAdapter.updateData(transactions)
                updateEmptyState()

                com.google.android.material.snackbar.Snackbar.make(
                    findViewById(android.R.id.content),
                    "Transaction deleted",
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).setAction("UNDO") {
                    transactions.add(transaction)
                    transactions.sortByDescending { it.id }
                    saveTransactions()
                    transactionAdapter.updateData(transactions)
                    updateEmptyState()
                }.show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveTransactions() {
        val sharedPreferences = getSharedPreferences("QuantumLedger", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putInt("transaction_count", transactions.size)

        transactions.forEachIndexed { index, transaction ->
            editor.putLong("transaction_${index}_id", transaction.id)
            editor.putString("transaction_${index}_amount", transaction.amount.toString())
            editor.putString("transaction_${index}_category", transaction.category)
            editor.putString("transaction_${index}_date", transaction.date)
            editor.putString("transaction_${index}_description", transaction.description)
            editor.putString("transaction_${index}_type", transaction.type.name)
            editor.putString("transaction_${index}_currency", transaction.currency)
            editor.putBoolean("transaction_${index}_tax", transaction.isTaxDeductible)
            editor.putBoolean("transaction_${index}_recurring", transaction.isRecurring)
        }

        editor.apply()
    }

    override fun onResume() {
        super.onResume()
        loadTransactions()
        transactionAdapter.updateData(transactions)
        updateEmptyState()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}