package com.example.handsfree_server

import android.content.Context
import android.media.AudioManager
import android.content.Context.AUDIO_SERVICE

import android.database.ContentObserver
import android.os.Handler
import android.util.Log


class VolumeObserver(context: Context, handler: Handler, val listener: OnVolumeChangeListener) : ContentObserver(handler) {

    val TAG = "VolumeObserver"
    var withListening:Boolean = true


    private val audioManager: AudioManager by lazy {
        context.getSystemService(AUDIO_SERVICE) as AudioManager
    }

    override fun deliverSelfNotifications(): Boolean {
        return false
    }

    override fun onChange(selfChange: Boolean) {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        Log.d(TAG, "Volume now $currentVolume")
        if(withListening){
            listener.onVolumeChanged(currentVolume)
        }

    }
}

interface OnVolumeChangeListener {
    fun onVolumeChanged(newValue: Int)
}