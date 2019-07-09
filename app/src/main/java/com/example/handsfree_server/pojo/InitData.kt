package com.example.handsfree_server.pojo


import com.google.gson.annotations.SerializedName
import java.util.*

data class InitData(
    @SerializedName("user_id")
    val userId: String, // web
    @SerializedName("lesson_id")
    val lessonId: Int, // 2
    @SerializedName("mother_language_id")
    val motherLanguageId: Int, // 1
    @SerializedName("target_language_id")
    val targetLanguageId: Int, // 2
    @SerializedName("word_list")
    val wordList: List<Int>,
    @SerializedName("timezone")
    val timeZone: String
)