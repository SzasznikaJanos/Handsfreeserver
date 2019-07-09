package com.example.handsfree_server.api.pojo

import com.google.gson.annotations.SerializedName

data class ReadBackResponse(
    @SerializedName("audio_link")
    val audioLink: String
)