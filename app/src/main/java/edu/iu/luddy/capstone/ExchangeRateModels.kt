package edu.iu.luddy.capstone

import com.google.gson.annotations.SerializedName

// Response from ExchangeRate API
data class ExchangeRateResponse(
    @SerializedName("result")
    val result: String,

    @SerializedName("base_code")
    val baseCode: String,

    @SerializedName("conversion_rates")
    val conversionRates: Map<String, Double>,

    @SerializedName("time_last_update_utc")
    val timeLastUpdate: String?
)

// Conversion result
data class ConversionResult(
    val fromCurrency: String,
    val toCurrency: String,
    val fromAmount: Double,
    val toAmount: Double,
    val rate: Double,
    val lastUpdate: String
)

// Popular currencies for the app
object PopularCurrencies {
    val currencies = listOf(
        "USD" to "US Dollar",
        "EUR" to "Euro",
        "GBP" to "British Pound",
        "JPY" to "Japanese Yen",
        "AUD" to "Australian Dollar",
        "CAD" to "Canadian Dollar",
        "CHF" to "Swiss Franc",
        "CNY" to "Chinese Yuan",
        "INR" to "Indian Rupee",
        "MXN" to "Mexican Peso",
        "BRL" to "Brazilian Real",
        "KRW" to "South Korean Won",
        "SGD" to "Singapore Dollar",
        "NZD" to "New Zealand Dollar",
        "HKD" to "Hong Kong Dollar"
    )

    val currencyCodes = currencies.map { it.first }
    val currencyNames = currencies.map { "${it.first} - ${it.second}" }
}