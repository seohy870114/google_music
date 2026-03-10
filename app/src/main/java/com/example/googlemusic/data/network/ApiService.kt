package com.example.googlemusic.data.network

import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

@Serializable
data class AnalyzeRequest(val url: String)

@Serializable
data class DownloadRequest(val url: String, val format: String)

@Serializable
data class VideoInfoResponse(
    val url: String? = null,
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

@Serializable
data class DownloadResponse(val task_id: String)

@Serializable
data class StatusResponse(
    val status: String,
    val progress: Float,
    val title: String? = null,
    val filename: String? = null,
    val message: String? = null
)

interface ApiService {
    @GET("/")
    suspend fun checkHealth(): HealthCheckResponse

    @POST("/analyze")
    suspend fun analyzeUrl(@Body request: AnalyzeRequest): VideoInfoResponse
    
    @POST("/download")
    suspend fun downloadMedia(@Body request: DownloadRequest): DownloadResponse
    
    @GET("/status/{task_id}")
    suspend fun getStatus(@Path("task_id") taskId: String): StatusResponse

    @Streaming
    @GET("/files/{filename}")
    suspend fun downloadFile(@Path("filename") filename: String): Response<ResponseBody>
}
