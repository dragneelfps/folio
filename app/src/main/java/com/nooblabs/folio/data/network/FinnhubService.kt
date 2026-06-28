package com.nooblabs.folio.data.network

import com.nooblabs.folio.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class FinnhubService(private val settingsRepository: SettingsRepository) {

    suspend fun fetchCurrentPrice(ticker: String): Double? = withContext(Dispatchers.IO) {
        val apiKey = settingsRepository.finnhubApiKey.value
        if (apiKey.isBlank() || apiKey == "dummy_key") {
            // Return null or handle dummy API key scenarios
            return@withContext null
        }

        try {
            val urlString = "https://finnhub.io/api/v1/quote?symbol=$ticker&token=$apiKey"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            try {
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)
                    // 'c' is the current price according to Finnhub quote API
                    if (jsonObject.has("c")) {
                        val currentPrice = jsonObject.getDouble("c")
                        if (currentPrice > 0.0) {
                            return@withContext currentPrice
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
