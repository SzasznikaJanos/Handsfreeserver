package com.example.handsfree_server.api

import com.google.gson.Gson


data class MainBody(val user_id: String, val user_input: String? = "",val location: String )
data class ReadbackBody(val language:String,val text:String? = null)

fun Any.toJson(): String = Gson().toJson(this)