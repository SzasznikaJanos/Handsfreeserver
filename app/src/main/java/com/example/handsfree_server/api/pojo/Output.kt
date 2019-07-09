package com.example.handsfree_server.api.pojo


import com.google.gson.annotations.SerializedName

data class Output(
    @SerializedName("audio")
    val audio: String, // https://handsfree.serveo.net/tts/da39a3ee5e6b4b0d3255bfef95601890afd80709

    @SerializedName("options")
    val options: List<String>,

    @SerializedName("text")
    val text: String, // Sorry, I don't know much about this topic yet, but I'm learning new things every day. Do you want to try another one?

    @SerializedName("type")
    val type: Int, // options

    @SerializedName("language")
    val language: Int
)