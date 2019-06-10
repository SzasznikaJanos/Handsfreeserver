package com.example.handsfree_server.pojo


import com.google.gson.annotations.SerializedName

data class ResponseFromMainAPi(
    @SerializedName("has_input")
    val hasInput: Boolean, // true

    @SerializedName("input_lang")
    val inputLang: String, // en-GB

    @SerializedName("output")
    val output: List<Output>,

    @SerializedName("hints")
    val inputHints: List<String> = listOf(),

    @SerializedName("is_correct")
    val isCorrect: Boolean? = null,

    @SerializedName("dialog_type")
    val dialogType: String?,

    @SerializedName("correct_answer")
    val readBackText: String? = null


)