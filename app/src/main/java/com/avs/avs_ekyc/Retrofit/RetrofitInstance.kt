package com.taskease.yksfoundation.Retrofit

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    val BASE_URL = "http://110.227.207.211:90/CKYCTEST_API/"

    val client = OkHttpClient.Builder()
        .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY // Enable logging
        })
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