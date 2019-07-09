package com.example.handsfree_server.pojo


import com.google.gson.annotations.SerializedName

data class InactivityResponse(
    @SerializedName("audio")
    val audio: String,
    @SerializedName("pause")
    val pause: Boolean, // false
    @SerializedName("text")
    val text: String,
    @SerializedName("type")
    val type: Int // 1
)