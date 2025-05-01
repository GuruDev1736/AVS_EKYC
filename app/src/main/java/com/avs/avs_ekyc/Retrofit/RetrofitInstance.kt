package com.taskease.yksfoundation.Retrofit

import com.avs.avs_ekyc.BuildConfig
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    private const val BASE_URL = BuildConfig.BASE_URL


    val client = OkHttpClient.Builder()
        .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY // Enable logging
        })
        .connectTimeout(60, TimeUnit.SECONDS) // Connection timeout
        .readTimeout(60, TimeUnit.SECONDS)    // Read timeout
        .writeTimeout(60, TimeUnit.SECONDS)   // Write timeout
        .build()

    val gson = GsonBuilder()
        .disableHtmlEscaping() // Prevents converting '=' to \u003d, etc.
        .create()

    fun getInstance(): ApiInterface {

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiInterface::class.java)
    }
}