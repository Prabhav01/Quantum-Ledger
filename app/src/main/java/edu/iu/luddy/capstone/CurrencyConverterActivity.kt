package edu.iu.luddy.capstone

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class CurrencyConverterActivity : AppCompatActivity() {

    private lateinit var fromCurrencySpinner: Spinner
    private lateinit var toCurrencySpinner: Spinner
    private lateinit var amountInput: TextInputEditText
    private lateinit var convertButton: MaterialButton
    private lateinit var resultText: MaterialTextView
    private lateinit var rateText: MaterialTextView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var errorText: TextView

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

        fromCurrencySpinner.setSelection(0)
        toCurrencySpinner.setSelection(2)

        fromCurrencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
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
                    val result = data.result
                    val rates = data.conversionRates

                    if (result == "success" && rates != null && rates.isNotEmpty()) {
                        currentRates = rates
                        hideError()
                        if (!amountInput.text.isNullOrEmpty()) {
                            performConversion()
                        }
                    } else {
                        showError("API error: ${result ?: "unknown"}")
                    }
                } else {
                    showError("Failed to load rates: HTTP ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ExchangeRateResponse>, t: Throwable) {
                showLoading(false)
                showError("Network error: ${t.message ?: "unknown error"}")
            }
        })
    }

    private fun performConversion() {
        val amountStr = amountInput.text?.toString()?.trim() ?: ""

        if (amountStr.isEmpty()) {
            amountInput.error = "Please enter an amount"
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            amountInput.error = "Please enter a valid amount"
            return
        }

        val rates = currentRates
        if (rates == null) {
            showError("Exchange rates not loaded yet")
            return
        }

        val fromIndex = fromCurrencySpinner.selectedItemPosition
        val toIndex = toCurrencySpinner.selectedItemPosition

        if (fromIndex !in PopularCurrencies.currencyCodes.indices ||
            toIndex !in PopularCurrencies.currencyCodes.indices
        ) {
            showError("Invalid currency selection")
            return
        }

        val fromCurrency = PopularCurrencies.currencyCodes[fromIndex]
        val toCurrency = PopularCurrencies.currencyCodes[toIndex]

        val toRate = rates[toCurrency]
        if (toRate == null) {
            showError("Rate not available for $toCurrency")
            return
        }

        val convertedAmount = amount * toRate

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
