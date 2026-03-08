package com.example.googlemusic.data.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@Serializable
data class AnalyzeRequest(val url: String)

@Serializable
data class VideoInfoResponse(
    val title: String?,
    val thumbnail: String?,
    val duration: Int?,
    val uploader: String?
)

@Serializable
data class HealthCheckResponse(
    val status: String,
    val message: String
)

interface ApiService {
    @GET("/")
    suspend fun checkHealth(): HealthCheckResponse

    @POST("/analyze")
    suspend fun analyzeUrl(@Body request: AnalyzeRequest): VideoInfoResponse
}
