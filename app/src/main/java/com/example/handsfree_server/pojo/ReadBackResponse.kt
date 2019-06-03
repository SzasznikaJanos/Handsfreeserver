package com.example.handsfree_server.pojo

import com.google.gson.annotations.SerializedName

data class ReadBackResponse(
    @SerializedName("audio_link")
    val audioLink: String
)