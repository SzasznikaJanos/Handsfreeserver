package com.example.handsfree_server.pojo

import com.google.gson.annotations.SerializedName

data class AnswerStatus(
    @SerializedName("is_correct")
    val isCorrect: Boolean,

    @SerializedName("correct_answer")
    val correctAnswer: String
)