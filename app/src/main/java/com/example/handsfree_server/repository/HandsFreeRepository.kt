package com.example.handsfree_server.repository

import android.util.Log
import com.example.handsfree_server.api.HandsFreeClient
import com.example.handsfree_server.api.MainBody
import com.example.handsfree_server.api.ReadbackBody
import com.example.handsfree_server.api.pojo.*
import com.example.handsfree_server.util.ServerException
import com.example.handsfree_server.util.ServerResult
import com.example.handsfree_server.util.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response

class HandsFreeRepository private constructor() {


    private val handsFreeApi by lazy {
        HandsFreeClient.client
    }

    companion object {
        private var INSTANCE: HandsFreeRepository? = null

        @Synchronized
        fun getInstance() = INSTANCE ?: HandsFreeRepository().also {
            INSTANCE = it
        }

    }

    suspend fun sendResponseToServer(mainBody: MainBody): ServerResult<MainResponse> {
        val response = handsFreeApi.postMainAsync(mainBody.toRequestBody())
        return safeFetch(response, "Failed to communicate with server!")
    }


    suspend fun initServerList(initData: InitData): ServerResult<InitResponse> {
        val response = handsFreeApi.initList(initData.toRequestBody())
        return safeFetch(response, "Failed to initialize the server.")
    }


    suspend fun initServer(initData: String): ServerResult<ResponseBody> {
        val response = handsFreeApi.initAsync(initData)
        return safeFetch(response, "Could not initialize the server!")
    }

    suspend fun getReadBackMessage(readbackBody: ReadbackBody): ServerResult<ReadBackResponse> {
        val response = handsFreeApi.readback(readbackBody.toRequestBody())
        return safeFetch(response, "Could not get back the readback message from the server")
    }

    suspend fun getFallBackMessage(user: String): ServerResult<ReadBackResponse> {
        val response = handsFreeApi.getFallBackMessage(user)
        return safeFetch(response, "Could not get back the fallback message from the server")
    }


    suspend fun getTimeOutMessage(user: String): ServerResult<InactivityResponse> {
        val response = handsFreeApi.getInactivityMessage(mapOf("session_id" to user).toRequestBody())
        return safeFetch(response, "Could not get back the TimeOut message from the server")
    }


    private fun <T> safeFetch(response: Response<T>, messageToDisplay: String): ServerResult<T> {
        return if (response.isSuccessful) {
            val data = response.body()
            if (data != null) {
                ServerResult.Success(data)
            } else {
                ServerResult.Error(
                    ServerException(
                        "The server responded with empty message",
                        "the response body is null",
                        response.code()
                    )
                )
            }
        } else {
            val error = ServerException(
                messageToDisplay,
                response.message(),
                response.code()
            )
            Log.e("Test", "safeFetch: ", error)
            ServerResult.Error(error)
        }
    }
}