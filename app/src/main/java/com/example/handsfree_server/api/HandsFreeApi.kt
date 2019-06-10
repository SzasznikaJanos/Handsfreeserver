package com.example.handsfree_server.api


import com.example.handsfree_server.pojo.ReadBackResponse
import com.example.handsfree_server.pojo.ResponseFromMainAPi



import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path


interface HandsFreeApi {

    @POST("/api")
    suspend fun postMainAsync(@Body body: RequestBody): Response<ResponseFromMainAPi>


    @GET("/init/{user}")
    suspend fun initAsync(@Path("user") user:String): Response<ResponseBody>

    @GET("/creds")
    suspend fun getCredentialsAsync(): Response<ResponseBody>

    @POST("/readback")
    suspend fun readback(@Body body: RequestBody): Response<ReadBackResponse>

    @GET("/fallback/{user}")
    suspend fun getFallBackMessage(@Path("user")user:String): Response<ReadBackResponse>


    @GET("/timeout/{user}")
    suspend fun getTimeOut(@Path("user")user:String): Response<ReadBackResponse>
}