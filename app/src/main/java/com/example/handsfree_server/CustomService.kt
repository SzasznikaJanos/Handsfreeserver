package com.example.handsfree_server

import android.app.Service
import android.content.Intent
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.Binder
import android.os.IBinder

class CustomService : Service() {

    interface ServiceCommunicationCallBack {
        fun startListening()
        fun stopListening()
    }

    companion object {
        fun fromBinder(binder: IBinder): CustomService = (binder as CustomBinder).service
    }

    override fun onBind(intent: Intent?): IBinder? {
        return CustomBinder()
    }

    inner class CustomBinder : Binder() {
        val service: CustomService
            get() = this@CustomService
    }

}