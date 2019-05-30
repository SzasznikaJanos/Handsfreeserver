package com.example.handsfree_server.util

import com.example.handsfree_server.api.toJson
import com.google.gson.Gson
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody



fun Any.toRequestBody(): RequestBody = RequestBody.create(MediaType.parse("application/json"), this.toJson())
