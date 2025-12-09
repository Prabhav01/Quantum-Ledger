package edu.iu.luddy.capstone

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.*

class CurrencyConverterActivity : AppCompatActivity() {

    private lateinit var fromCurrencySpinner: Spinner
    private lateinit var toCurrencySpinner: Spinner
    private lateinit var amountInput: TextInputEditText
    private lateinit var convertButton: MaterialButton
    private lateinit var resultText: MaterialTextView
    private lateinit var rateText: MaterialTextView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var errorText: MaterialTextView

    private val api = ExchangeRateApi.create()
    private var currentRates: Map<String, Double>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_currency_converter)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Currency Converter"

        initializeViews()
        setupSpinners()
        setupConvertButton()

        // Load initial rates for USD
        loadExchangeRates("USD")
    }

    private fun initializeViews() {
        fromCurrencySpinner = findViewById(R.id.fromCurrencySpinner)
        toCurrencySpinner = findViewById(R.id.toCurrencySpinner)
        amountInput = findViewById(R.id.amountInput)
        convertButton = findViewById(R.id.convertButton)
        resultText = findViewById(R.id.resultText)
        rateText = findViewById(R.id.rateText)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        errorText = findViewById(R.id.errorText)
    }

    private fun setupSpinners() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            PopularCurrencies.currencyNames
        )

        fromCurrencySpinner.adapter = adapter
        toCurrencySpinner.adapter = adapter

        // Set default: USD to GBP (as shown in screenshot)
        fromCurrencySpinner.setSelection(0) // USD
        toCurrencySpinner.setSelection(2)   // GBP

        // Reload rates when base currency changes
        fromCurrencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val baseCurrency = PopularCurrencies.currencyCodes[position]
                loadExchangeRates(baseCurrency)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupConvertButton() {
        convertButton.setOnClickListener {
            performConversion()
        }
    }

    private fun loadExchangeRates(baseCurrency: String) {
        showLoading(true)
        hideError()

        api.getExchangeRates(baseCurrency).enqueue(object : Callback<ExchangeRateResponse> {
            override fun onResponse(
                call: Call<ExchangeRateResponse>,
                response: Response<ExchangeRateResponse>
            ) {
                showLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    // Check if the API call was successful
                    if (data.result == "success") {
                        currentRates = data.conversionRates
                        hideError()

                        // Auto-convert if amount is entered
                        if (!amountInput.text.isNullOrEmpty()) {
                            performConversion()
                        }
                    } else {
                        showError("API returned error: ${data.result}")
                    }
                } else {
                    showError("Failed to load rates. Status: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ExchangeRateResponse>, t: Throwable) {
                showLoading(false)
                showError("Network error: ${t.message}\n\nCheck your internet connection")
            }
        })
    }

    private fun performConversion() {
        val amountStr = amountInput.text.toString()

        if (amountStr.isEmpty()) {
            amountInput.error = "Please enter an amount"
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            amountInput.error = "Please enter a valid amount"
            return
        }

        if (currentRates == null) {
            showError("Exchange rates not loaded. Please wait...")
            return
        }

        val fromPosition = fromCurrencySpinner.selectedItemPosition
        val toPosition = toCurrencySpinner.selectedItemPosition

        val fromCurrency = PopularCurrencies.currencyCodes[fromPosition]
        val toCurrency = PopularCurrencies.currencyCodes[toPosition]

        val toRate = currentRates!![toCurrency]
        if (toRate == null) {
            showError("Rate not available for $toCurrency")
            return
        }

        val convertedAmount = amount * toRate

        // Display results
        val format = NumberFormat.getNumberInstance(Locale.US)
        format.minimumFractionDigits = 2
        format.maximumFractionDigits = 2

        resultText.text = "${format.format(convertedAmount)} $toCurrency"
        rateText.text = "1 $fromCurrency = ${format.format(toRate)} $toCurrency"

        resultText.visibility = View.VISIBLE
        rateText.visibility = View.VISIBLE
        hideError()
    }

    private fun showLoading(show: Boolean) {
        loadingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        convertButton.isEnabled = !show
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
        resultText.visibility = View.GONE
        rateText.visibility = View.GONE
    }

    private fun hideError() {
        errorText.visibility = View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}