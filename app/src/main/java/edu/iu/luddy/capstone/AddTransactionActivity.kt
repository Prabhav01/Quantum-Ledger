package edu.iu.luddy.capstone

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var amountInput: TextInputEditText
    private lateinit var categorySpinner: Spinner
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var dateButton: MaterialButton
    private lateinit var typeRadioGroup: RadioGroup
    private lateinit var taxDeductibleCheckbox: CheckBox
    private lateinit var recurringCheckbox: CheckBox
    private lateinit var saveButton: MaterialButton

    private var selectedDate = Calendar.getInstance()
    private var transactionType = TransactionType.EXPENSE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        // Enable back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Add Transaction"

        initializeViews()
        setupSpinner()
        setupDatePicker()
        setupRadioGroup()
        setupSaveButton()

        // Check if editing existing transaction
        checkForEditMode()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save form data during rotation
        outState.putString("amount", amountInput.text.toString())
        outState.putString("description", descriptionInput.text.toString())
        outState.putLong("date", selectedDate.timeInMillis)
        outState.putString("type", transactionType.name)
        outState.putBoolean("taxDeductible", taxDeductibleCheckbox.isChecked)
        outState.putBoolean("recurring", recurringCheckbox.isChecked)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Restore form data after rotation
        amountInput.setText(savedInstanceState.getString("amount", ""))
        descriptionInput.setText(savedInstanceState.getString("description", ""))
        selectedDate.timeInMillis = savedInstanceState.getLong("date")
        updateDateButton()
        taxDeductibleCheckbox.isChecked = savedInstanceState.getBoolean("taxDeductible")
        recurringCheckbox.isChecked = savedInstanceState.getBoolean("recurring")
    }

    private fun initializeViews() {
        amountInput = findViewById(R.id.amountInput)
        categorySpinner = findViewById(R.id.categorySpinner)
        descriptionInput = findViewById(R.id.descriptionInput)
        dateButton = findViewById(R.id.dateButton)
        typeRadioGroup = findViewById(R.id.typeRadioGroup)
        taxDeductibleCheckbox = findViewById(R.id.taxDeductibleCheckbox)
        recurringCheckbox = findViewById(R.id.recurringCheckbox)
        saveButton = findViewById(R.id.saveButton)
    }

    private fun setupSpinner() {
        val categories = if (transactionType == TransactionType.EXPENSE) {
            TransactionCategories.EXPENSE_CATEGORIES
        } else {
            TransactionCategories.INCOME_CATEGORIES
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )
        categorySpinner.adapter = adapter
    }

    private fun setupDatePicker() {
        updateDateButton()

        dateButton.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDate.set(year, month, dayOfMonth)
                    updateDateButton()
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }
    }

    private fun updateDateButton() {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        dateButton.text = dateFormat.format(selectedDate.time)
    }

    private fun setupRadioGroup() {
        typeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            transactionType = if (checkedId == R.id.expenseRadio) {
                TransactionType.EXPENSE
            } else {
                TransactionType.INCOME
            }
            setupSpinner() // Update categories based on type
        }
    }

    private fun setupSaveButton() {
        saveButton.setOnClickListener {
            if (validateInput()) {
                saveTransaction()
            }
        }
    }

    private fun validateInput(): Boolean {
        val amount = amountInput.text.toString()

        if (amount.isEmpty()) {
            amountInput.error = "Please enter an amount"
            return false
        }

        if (amount.toDoubleOrNull() == null || amount.toDouble() <= 0) {
            amountInput.error = "Please enter a valid amount"
            return false
        }

        return true
    }

    private fun saveTransaction() {
        val amount = amountInput.text.toString().toDouble()
        val category = categorySpinner.selectedItem.toString()
        val description = descriptionInput.text.toString()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val date = dateFormat.format(selectedDate.time)

        val transaction = Transaction(
            id = System.currentTimeMillis(),
            amount = amount,
            category = category,
            date = date,
            description = description,
            type = transactionType,
            currency = "USD",
            isTaxDeductible = taxDeductibleCheckbox.isChecked,
            isRecurring = recurringCheckbox.isChecked
        )

        // Save to SharedPreferences
        saveTransactionToPreferences(transaction)

        // Show success message
        Toast.makeText(this, "Transaction saved!", Toast.LENGTH_SHORT).show()

        // Return to MainActivity
        finish()
    }

    private fun saveTransactionToPreferences(transaction: Transaction) {
        val sharedPreferences = getSharedPreferences("QuantumLedger", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Get current transaction count
        val count = sharedPreferences.getInt("transaction_count", 0)

        // Check if editing existing transaction
        val transactionId = intent.getLongExtra("transaction_id", -1)
        val index = if (transactionId != -1L) {
            // Find index of existing transaction
            (0 until count).find {
                sharedPreferences.getLong("transaction_${it}_id", 0L) == transactionId
            } ?: count
        } else {
            count
        }

        // Save transaction
        editor.putLong("transaction_${index}_id", transaction.id)
        editor.putString("transaction_${index}_amount", transaction.amount.toString())
        editor.putString("transaction_${index}_category", transaction.category)
        editor.putString("transaction_${index}_date", transaction.date)
        editor.putString("transaction_${index}_description", transaction.description)
        editor.putString("transaction_${index}_type", transaction.type.name)
        editor.putString("transaction_${index}_currency", transaction.currency)
        editor.putBoolean("transaction_${index}_tax", transaction.isTaxDeductible)
        editor.putBoolean("transaction_${index}_recurring", transaction.isRecurring)

        // Update count if new transaction
        if (transactionId == -1L) {
            editor.putInt("transaction_count", count + 1)
        }

        editor.apply()
    }

    private fun checkForEditMode() {
        // Check if editing existing transaction
        val transactionId = intent.getLongExtra("transaction_id", -1)
        if (transactionId != -1L) {
            supportActionBar?.title = "Edit Transaction"

            // Load transaction data
            val amount = intent.getDoubleExtra("transaction_amount", 0.0)
            val category = intent.getStringExtra("transaction_category") ?: ""
            val description = intent.getStringExtra("transaction_description") ?: ""
            val type = intent.getStringExtra("transaction_type") ?: "EXPENSE"

            // Populate fields
            amountInput.setText(amount.toString())
            descriptionInput.setText(description)
            transactionType = TransactionType.valueOf(type)

            if (transactionType == TransactionType.INCOME) {
                typeRadioGroup.check(R.id.incomeRadio)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}