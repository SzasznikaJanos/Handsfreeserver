package com.example.handsfree_server.api


import com.example.handsfree_server.pojo.ResponseFromMainAPi

import kotlinx.coroutines.Deferred
import okhttp3.RequestBody
import okhttp3.ResponseBody

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST


interface HandsFreeApi {

    @POST("/api")
    fun postMainAsync(@Body body:RequestBody): Deferred<ResponseFromMainAPi>


    @GET("/init/test")
    fun initAsync(): Deferred<ResponseBody>

    @GET("/creds")
    fun getCredentialsAsync():Deferred<ResponseBody>
}