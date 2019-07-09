package com.example.handsfree_server.api


import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.Executors

object HandsFreeClient {

    private const val BASE_URL = "http://hf-staging.kzvvuvzqwb.eu-west-1.elasticbeanstalk.com/"


    private val retrofit by lazy {
        Retrofit.Builder().baseUrl(BASE_URL)

            .addConverterFactory(GsonConverterFactory.create())
            .callbackExecutor(Executors.newSingleThreadExecutor())
            .build()
    }

    val client: HandsFreeApi by lazy { retrofit.create(HandsFreeApi::class.java) }
}