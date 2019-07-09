package com.example.handsfree_server.api.pojo


import com.google.gson.annotations.SerializedName

data class InitResponse(
    @SerializedName("session_id")
    val sessionId: String // 7e121d4e781e46bba6fe60d8c8f1acf8
)