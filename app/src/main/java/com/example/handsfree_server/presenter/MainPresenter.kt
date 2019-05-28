package com.example.handsfree_server.presenter

import android.content.Context
import android.util.Log
import com.example.handsfree_server.R
import com.example.handsfree_server.api.HandsfreeClient
import com.example.handsfree_server.api.MainBody
import com.example.handsfree_server.model.AudioPlayer
import com.example.handsfree_server.model.SpeechItem
import com.example.handsfree_server.pojo.ResponseFromMainAPi

import com.example.handsfree_server.speechrecognizer.SpeechRecognizer
import com.example.handsfree_server.speechrecognizer.SpeechRecognizer.Companion.recognizedText
import com.example.handsfree_server.speechrecognizer.SpeechResponse
import com.example.handsfree_server.util.TAG
import com.example.handsfree_server.util.toRequestBody

import com.example.handsfree_server.view.MainView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class MainPresenter(private val audioPlayer: AudioPlayer, private val mainView: MainView) : CoroutineScope {

    private var cachedResponse: ResponseFromMainAPi? = null

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main


    val handsFreeApi = HandsfreeClient.client


    private var cancelled = false

    private val speechRecognizer: SpeechRecognizer = SpeechRecognizer()

    private val speechListener: SpeechRecognizer.SpeechListener = object : SpeechRecognizer.SpeechListener {

        override fun onSpeechRecognized(speechResponse: SpeechResponse) {
            if (cancelled) return
            Log.d(TAG, "onSpeechRecognized: ${speechResponse.speechResponseAsText}")
            recognizedText = speechResponse.speechResponseAsText
            if (!speechResponse.isFinal) {
                mainView.addRecognizedSpeechBubble(recognizedText, false)

            } else {
                stopSpeechListening()
            }

        }

        override fun onBind() {
            audioPlayer.audioPlayerCallBack = object : AudioPlayer.AudioPlayerCallbacks {
                override fun onAudioEnd(audioId: Int) {
                    when (audioId) {
                        AudioPlayer.AUDIO_ID_START_RECOGNITION -> startSpeechListening()
                        AudioPlayer.AUDIO_ID_PLAY_FEEDBACK -> playAudioFeedBack(
                            cachedResponse!!.isCorrect!!,
                            AudioPlayer.AUDIO_ID_PLAY_OUTPUTS
                        )
                        AudioPlayer.AUDIO_ID_PLAY_OUTPUTS -> playOutPuts()
                        AudioPlayer.AUDIO_ID_NEW_REQUEST -> emptyRequest()
                    }
                }

                override fun onAudioStart(speechItem: SpeechItem) = mainView.addTTSBubble(speechItem)
            }
            start()
        }

        override fun onCompleted(recognizedText: String) {
            Log.d(TAG, "onCompleted: recognizedText = $recognizedText")
            mainView.addRecognizedSpeechBubble(recognizedText, true)
            launch {
                val response = handsFreeApi.postMainAsync(MainBody("test", recognizedText).toRequestBody()).await()

                handleResponse(response)
            }
        }
    }


    private fun handleContinuousResponse(userResponse: String) {
        cachedResponse?.let {
            if (!it.inputHints.isNullOrEmpty()) {
                val targetText = it.inputHints[0]

                val isMatching = userResponse.toLowerCase() == targetText.toLowerCase()

                Log.d(TAG, "handleContinuousResponse: response: ${userResponse.toLowerCase()}, terget: ${targetText.toLowerCase()}")
                if (isMatching) {
                    stopSpeechListening()
                }
            }
        }
    }

    private fun handleResponse(response: ResponseFromMainAPi) {
        cachedResponse = response

        if (response.isCorrect != null) {
            mainView.updateQuizResultImage(response.isCorrect)
            playAudioFeedBack(response.isCorrect, AudioPlayer.AUDIO_ID_PLAY_OUTPUTS)
        } else {
            playOutPuts()
        }
    }

    private fun emptyRequest() {
        launch {
            val response = handsFreeApi.postMainAsync(MainBody("test").toRequestBody()).await()
            handleResponse(response)
        }
    }


    fun bind(applicationContext: Context) = speechRecognizer.bindService(applicationContext, speechListener)

    fun stopSpeechListening() {
        cancelled = true
        speechRecognizer.stopVoiceRecorder()
        mainView.hideMicInput()
        playMicrophoneSound()
    }

    fun start() {
        launch {
            handsFreeApi.initAsync().await()
            val response = handsFreeApi.postMainAsync(MainBody("test").toRequestBody()).await()
            cachedResponse = response
            audioPlayer.play(response.output, AudioPlayer.AUDIO_ID_START_RECOGNITION)
        }
    }

    fun startSpeechListening() {
        cancelled = false
        speechRecognizer.startVoiceRecorder(cachedResponse?.inputLang ?: "", true, cachedResponse?.inputHints?: emptyList())
        mainView.showMicInput()
        playMicrophoneSound()

    }

    fun playAudioFeedBack(isCorrect: Boolean, audioId: Int) =
        if (isCorrect) audioPlayer.play(R.raw.fast_correct, audioId)
        else audioPlayer.play(R.raw.fast_incorrect, audioId)

    fun playOutPuts() {
        val audioId =
            if (cachedResponse?.hasInput == true) AudioPlayer.AUDIO_ID_START_RECOGNITION else AudioPlayer.AUDIO_ID_NEW_REQUEST

        audioPlayer.play(cachedResponse!!.output, audioId)
    }


    private fun playMicrophoneSound() = audioPlayer.play(R.raw.speak, -1)


}
