package edu.iu.luddy.capstone

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textview.MaterialTextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var balanceText: MaterialTextView
    private lateinit var totalIncomeText: MaterialTextView
    private lateinit var totalExpenseText: MaterialTextView
    private lateinit var recentTransactionsRecyclerView: RecyclerView
    private lateinit var viewAllButton: MaterialButton
    private lateinit var fabAddTransaction: FloatingActionButton
    private lateinit var testCurrencyButton: MaterialButton

    private lateinit var transactionAdapter: TransactionAdapter
    private var transactions = mutableListOf<Transaction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        initializeViews()

        // Setup RecyclerView first
        setupRecyclerView()

        // Load saved data
        loadTransactions()

        // Update UI with loaded data
        updateFinancialSummary()
        refreshTransactionsList()

        // Setup click listeners
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this activity
        loadTransactions()
        updateFinancialSummary()
        refreshTransactionsList()
    }

    override fun onPause() {
        super.onPause()
        // Save transactions when app goes to background
        saveTransactions()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current balance for rotation
        outState.putDouble("balance", calculateBalance())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Restore balance after rotation
        val balance = savedInstanceState.getDouble("balance", 0.0)
        balanceText.text = formatCurrency(balance)
    }

    private fun initializeViews() {
        balanceText = findViewById(R.id.balanceText)
        totalIncomeText = findViewById(R.id.totalIncomeText)
        totalExpenseText = findViewById(R.id.totalExpenseText)
        recentTransactionsRecyclerView = findViewById(R.id.recentTransactionsRecyclerView)
        viewAllButton = findViewById(R.id.viewAllButton)
        fabAddTransaction = findViewById(R.id.fabAddTransaction)
        testCurrencyButton = findViewById(R.id.testCurrencyButton)
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            mutableListOf(), // Start with empty list
            onEdit = { transaction -> editTransaction(transaction) },
            onDelete = { transaction -> deleteTransaction(transaction) }
        )

        recentTransactionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = transactionAdapter
        }
    }

    private fun refreshTransactionsList() {
        // Update adapter with latest 5 transactions
        transactionAdapter.updateData(transactions.take(5).toMutableList())
    }

    private fun setupClickListeners() {
        fabAddTransaction.setOnClickListener {
            val intent = Intent(this, AddTransactionActivity::class.java)
            startActivity(intent)
        }

        viewAllButton.setOnClickListener {
            val intent = Intent(this, TransactionHistoryActivity::class.java)
            startActivity(intent)
        }

        testCurrencyButton.setOnClickListener {
            val intent = Intent(this, CurrencyConverterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadTransactions() {
        val sharedPreferences = getSharedPreferences("QuantumLedger", MODE_PRIVATE)

        // Load transaction count
        val count = sharedPreferences.getInt("transaction_count", 0)

        transactions.clear()

        // Load each transaction
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

        // Sort by ID descending (newest first)
        transactions.sortByDescending { it.id }
    }

    private fun saveTransactions() {
        val sharedPreferences = getSharedPreferences("QuantumLedger", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Save transaction count
        editor.putInt("transaction_count", transactions.size)

        // Save each transaction
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

    private fun updateFinancialSummary() {
        val totalIncome = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        val totalExpense = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val balance = totalIncome - totalExpense

        balanceText.text = formatCurrency(balance)
        totalIncomeText.text = formatCurrency(totalIncome)
        totalExpenseText.text = formatCurrency(totalExpense)
    }

    private fun calculateBalance(): Double {
        val totalIncome = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        val totalExpense = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        return totalIncome - totalExpense
    }

    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale.US)
        return format.format(amount)
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
        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                transactions.remove(transaction)
                updateFinancialSummary()
                refreshTransactionsList()
                saveTransactions()

                // Show Snackbar with UNDO
                com.google.android.material.snackbar.Snackbar.make(
                    findViewById(android.R.id.content),
                    "Transaction deleted",
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).setAction("UNDO") {
                    transactions.add(transaction)
                    transactions.sortByDescending { it.id }
                    updateFinancialSummary()
                    refreshTransactionsList()
                    saveTransactions()
                }.show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_currency_converter -> {
                val intent = Intent(this, CurrencyConverterActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_settings -> {
                android.widget.Toast.makeText(this, "Settings - Coming Soon", android.widget.Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_about -> {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}