package com.example.handsfree_server.speechrecognizer

import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log

import com.example.handsfree_server.util.TAG


class SpeechRecognizer {
    companion object {
        var recognizedText: String = ""
        var isSpeechRecognizerActive = false


    }

    interface SpeechListener {
        fun onSpeechRecognized(speechResponse: SpeechResponse)
        fun onBind()
        fun onCompleted(recognizedText:String)
    }

    var speechListener: SpeechListener? = null


    private var speechService: SpeechService? = null

    private var voiceRecorder: VoiceRecorder? = null

    fun bindService(applicationContext: Context, speechListener: SpeechListener) {
        this.speechListener = speechListener
        val serviceIntent = Intent(applicationContext, SpeechService::class.java)
        applicationContext.bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE)
    }

    private lateinit var languageTag: String

    var isSingleUtterance = true

    private var hints: List<String>? = null

    private val voiceRecorderCallBacks = object : VoiceRecorder.Callback {
        override fun onVoiceStart() {
            Log.d(TAG, "onVoiceStart: Called")
            speechService?.startRecognitionRequest(getSampleRate(), languageTag, isSingleUtterance, hints)
        }

        override fun onVoice(data: ByteArray, size: Int) {
            speechService?.recognize(data, size)
        }

        override fun onVoiceEnd() {
            Log.d(TAG, "onVoiceEnd: Called")
            speechService?.finishRecognizing()

        }
    }

    private fun getSampleRate() = voiceRecorder?.sampleRate ?: 0

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceDisconnected(name: ComponentName?) {
            speechService = null
            Log.e(TAG, "onServiceDisconnected: Service Disconnected!")
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service == null) {
                Log.e(TAG, "onServiceConnected: Service is null! cannot continue")
                return
            }
            val listener = speechListener
            if (listener == null) {
                Log.e(TAG, "Speech listener is null!")
                return
            }

            speechService = SpeechService.from(service, listener)
            speechListener?.onBind()
        }

    }


    fun startVoiceRecorder(languageTag: String, isSingleUtterance: Boolean, hints: List<String>? = mutableListOf()) {
        updateOptions(languageTag, isSingleUtterance, hints)
        prepareAndStartVoiceRecorder()
    }

    fun stopVoiceRecorder() {
        voiceRecorder?.stop()
        voiceRecorder = null
        isSpeechRecognizerActive = false
    }

    private fun prepareAndStartVoiceRecorder() {
        voiceRecorder?.stop()
        voiceRecorder = VoiceRecorder(voiceRecorderCallBacks)
        voiceRecorder?.start()
        isSpeechRecognizerActive = true
    }

    private fun updateOptions(languageTag: String, isSingleUtterance: Boolean, hints: List<String>?) {
        this.languageTag = languageTag
        this.isSingleUtterance = isSingleUtterance
        this.hints = hints
    }

}