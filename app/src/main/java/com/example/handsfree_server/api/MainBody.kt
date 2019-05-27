package com.example.handsfree_server.api

import com.google.gson.Gson

data class MainBody(val user_id: String, val user_input: String? = "")


fun Any.toJson(): String = Gson().toJson(this)