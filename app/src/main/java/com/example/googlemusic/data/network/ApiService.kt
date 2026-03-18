package com.example.googlemusic.data.network

import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

@Serializable
data class AnalyzeRequest(val url: String)

@Serializable
data class DownloadRequest(val url: String, val format: String)

@Serializable
data class DriveUploadRequest(val task_id: String, val access_token: String)

@Serializable
data class VideoInfoResponse(
    val url: String? = null,
    val title: String?,
    val thumbnail: String?,
    val duration: Int?,
    val uploader: String?,
    val video_id: String? = null
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
    val ext: String? = null,
    val message: String? = null,
    val has_subtitles: Boolean = false,
    val subtitle_filename: String? = null
)

@Serializable
data class GenericResponse(val status: String)

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
    @GET("/files/{task_id}/{filename}")
    suspend fun downloadFile(
        @Path("task_id") taskId: String,
        @Path("filename") filename: String
    ): Response<ResponseBody>

    @POST("/upload/drive")
    suspend fun uploadToDrive(@Body request: DriveUploadRequest): GenericResponse
}
