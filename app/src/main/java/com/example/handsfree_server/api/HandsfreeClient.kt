package com.example.handsfree_server.api

import com.google.api.AnnotationsProto.http
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.Executors

object HandsfreeClient {

    private const val BASE_URL = "https://fca8b038.ngrok.io//"


    private val retrofit by lazy {
        Retrofit.Builder().baseUrl(BASE_URL)

            .addConverterFactory(GsonConverterFactory.create())
            .callbackExecutor(Executors.newSingleThreadExecutor())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
    }

    val client: HandsFreeApi by lazy { retrofit.create(HandsFreeApi::class.java) }
}