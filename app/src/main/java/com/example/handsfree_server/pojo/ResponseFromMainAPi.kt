package com.example.handsfree_server.pojo


import com.google.gson.annotations.SerializedName

data class ResponseFromMainAPi(
    @SerializedName("has_input")
    val hasInput: Boolean, // true

    @SerializedName("input_lang")
    val inputLang: Int, // en-GB

    @SerializedName("output")
    val output: List<Output>,

    @SerializedName("hints")
    val inputHints: List<String> = listOf(),

    @SerializedName("dialog_type")
    val dialogType: String?,

    @SerializedName("answer_status")
    val answerStatus:AnswerStatus? = null


)