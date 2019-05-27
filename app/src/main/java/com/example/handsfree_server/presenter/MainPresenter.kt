package com.example.handsfree_server.presenter

import android.content.Context
import android.util.Log
import com.example.handsfree_server.api.HandsfreeClient
import com.example.handsfree_server.api.MainBody
import com.example.handsfree_server.model.AudioPlayer
import com.example.handsfree_server.model.SpeechItem
import com.example.handsfree_server.model.SpeechType
import com.example.handsfree_server.speechrecognizer.SpeechRecognizer
import com.example.handsfree_server.speechrecognizer.SpeechResponse
import com.example.handsfree_server.util.TAG
import com.example.handsfree_server.util.toRequestBody
import com.example.handsfree_server.view.MainView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class MainPresenter(private val audioPlayer: AudioPlayer, private val mainView: MainView) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private val handsFreeClient = HandsfreeClient.client


    private val speechRecognizer = SpeechRecognizer()


    private var isItemAdded = false

    private var cachedSpeechResponse: SpeechResponse? = null

    private lateinit var languageTag: String

    private var hints: List<String> = listOf()

    private val speechListener = object : SpeechRecognizer.SpeechListener {

        override fun onSpeechRecognized(speechResponse: SpeechResponse) {
            Log.d("Speech recognizer", "onSpeechRecognized: ${speechResponse.speechResponseAsText}")
            addOrUpdateSpeechBubble(speechResponse.speechResponseAsText)
            cachedSpeechResponse = speechResponse
            SpeechRecognizer.recognizedText = speechResponse.speechResponseAsText

            if (speechResponse.isFinal) {
                stopVoiceRecorder()
            }
        }


        override fun onBind() {
            launch {
                audioPlayer.audioPlayerCallBack = object : AudioPlayer.AudioPlayerCallbacks {
                    override fun onAudioEnd() = startSpeechListening()
                    override fun onAudioStart(speechItem: SpeechItem) = mainView.addSpeechBubble(speechItem)
                }

                handsFreeClient.initAsync().await()
                val response = handsFreeClient.postMainAsync(MainBody("test").toRequestBody()).await()
                languageTag = response.inputLang

                audioPlayer.play(response.output)
            }
        }

        override fun stopListening() {
            Log.d(TAG, "stopListening: called")
            handleFinalSpeechResponse()
            isItemAdded = false
        }

    }
    fun handleFinalSpeechResponse() {
        cachedSpeechResponse?.let {

            launch {
                val response =
                    handsFreeClient.postMainAsync(MainBody("test", it.speechResponseAsText).toRequestBody()).await()
                Log.d(TAG, "stopListening: $response")
                if (response.isCorrect != null)
                    mainView.updateQuizResultImage(response.isCorrect)

                audioPlayer.play(response.output)
                languageTag = response.inputLang
                hints = response.inputHints
                mainView.silentUpdateLastSpeechItem(it.speechResponseAsText)
            }
        }
    }

    fun startSpeechListening() {
        speechRecognizer.startVoiceRecorder(languageTag, true, hints)
        mainView.showMicInput()
    }

    private fun stopVoiceRecorder(hideButtons: Boolean = true) {
        speechRecognizer.stopVoiceRecorder()
        mainView.hideMicInput()

    }

    fun bind(applicationContext: Context) {
        speechRecognizer.bindService(applicationContext, speechListener)
    }

    private fun addOrUpdateSpeechBubble(userResponse: String) {
        if (!isItemAdded) {
            isItemAdded = true
            mainView.addSpeechBubble(SpeechItem(userResponse, type = SpeechType.SENT))
        } else {
            mainView.updateView(userResponse)
        }
    }




}
