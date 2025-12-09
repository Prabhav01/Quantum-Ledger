package edu.iu.luddy.capstone

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface ExchangeRateApi {

    @GET("latest/{currency}")
    fun getExchangeRates(@Path("currency") baseCurrency: String): Call<ExchangeRateResponse>

    companion object {
        private const val BASE_URL = "https://open.er-api.com/v6/"

        fun create(): ExchangeRateApi {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(ExchangeRateApi::class.java)
        }
    }
}