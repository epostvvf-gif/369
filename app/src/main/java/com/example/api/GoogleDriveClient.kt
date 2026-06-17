package com.example.api

import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class DriveFileResponse(
    val id: String,
    val name: String,
    val mimeType: String? = null,
    val size: String? = null,
    val modifiedTime: String? = null
)

@JsonClass(generateAdapter = true)
data class DriveFilesListResponse(
    val files: List<DriveFileResponse>? = null,
    val nextPageToken: String? = null
)

interface GoogleDriveService {
    @GET("drive/v3/files")
    suspend fun listFiles(
        @Header("Authorization") authHeader: String,
        @Query("pageSize") pageSize: Int = 100,
        @Query("fields") fields: String = "nextPageToken, files(id, name, size, mimeType, modifiedTime)",
        @Query("q") q: String = "trashed = false",
        @Query("orderBy") orderBy: String = "modifiedTime desc"
    ): DriveFilesListResponse

    @GET("drive/v3/files/{fileId}")
    suspend fun downloadFile(
        @Header("Authorization") authHeader: String,
        @Path("fileId") fileId: String,
        @Query("alt") alt: String = "media"
    ): ResponseBody
}

object GoogleDriveClient {
    private const val BASE_URL = "https://www.googleapis.com/"

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    val service: GoogleDriveService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GoogleDriveService::class.java)
    }
}
