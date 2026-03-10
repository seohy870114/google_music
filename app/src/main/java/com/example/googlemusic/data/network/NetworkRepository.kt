package com.example.googlemusic.data.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

class NetworkRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    fun getApiService(baseUrl: String): ApiService? {
        if (baseUrl.isEmpty()) return null
        
        // Ensure the URL starts with http:// and ends with a /
        val formattedUrl = when {
            baseUrl.startsWith("http://") || baseUrl.startsWith("https://") -> baseUrl
            else -> "http://$baseUrl"
        }.let { if (it.endsWith("/")) it else "$it/" }
        
        // Also need to handle cases where only IP is provided (default port 8000)
        val finalUrl = if (formattedUrl.indexOf(":", 7) == -1) {
            formattedUrl.replace("/", ":8000/")
        } else {
            formattedUrl
        }

        return try {
            Retrofit.Builder()
                .baseUrl(finalUrl)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(ApiService::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
