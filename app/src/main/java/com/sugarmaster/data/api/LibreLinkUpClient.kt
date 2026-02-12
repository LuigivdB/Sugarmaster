package com.sugarmaster.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object LibreLinkUpClient {

    private const val LLU_VERSION = "4.12.0"
    private const val LLU_PRODUCT = "llu.android"

    private var authToken: String? = null
    private var accountId: String? = null
    private var currentRegion: String = ""
    private var api: LibreLinkUpApi? = null

    fun setAuth(token: String?, accountId: String?) {
        this.authToken = token
        this.accountId = accountId
    }

    fun clearAuth() {
        authToken = null
        accountId = null
        api = null
    }

    fun setRegion(region: String) {
        if (region != currentRegion) {
            currentRegion = region
            api = null // Force rebuild with new base URL
        }
    }

    fun getApi(): LibreLinkUpApi {
        api?.let { return it }

        val headerInterceptor = Interceptor { chain ->
            val builder = chain.request().newBuilder()
                .header("Content-Type", "application/json")
                .header("Cache-Control", "no-cache")
                .header("Connection", "Keep-Alive")
                .header("Accept-Encoding", "gzip")
                .header("product", LLU_PRODUCT)
                .header("version", LLU_VERSION)

            authToken?.let { builder.header("Authorization", "Bearer $it") }
            accountId?.let { builder.header("Account-Id", it) }

            chain.proceed(builder.build())
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val baseUrl = buildBaseUrl()

        val newApi = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LibreLinkUpApi::class.java)

        api = newApi
        return newApi
    }

    private fun buildBaseUrl(): String {
        return if (currentRegion.isNotBlank()) {
            "https://api-$currentRegion.libreview.io/"
        } else {
            "https://api.libreview.io/"
        }
    }
}
