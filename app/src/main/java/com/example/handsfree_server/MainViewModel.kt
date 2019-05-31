package com.example.handsfree_server

import android.app.Application

import androidx.lifecycle.AndroidViewModel

import androidx.lifecycle.MutableLiveData
import com.example.handsfree_server.api.HandsfreeClient
import com.example.handsfree_server.api.MainBody
import com.example.handsfree_server.model.AudioPlayer
import com.example.handsfree_server.model.SpeechItem
import com.example.handsfree_server.model.SpeechType
import com.example.handsfree_server.pojo.ResponseFromMainAPi
import com.example.handsfree_server.speechrecognizer.SpeechRecognizer
import com.example.handsfree_server.speechrecognizer.SpeechResponse
import com.example.handsfree_server.speechrecognizer.SpeechService
import com.example.handsfree_server.util.toRequestBody
import com.example.handsfree_server.view.MainView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class MainViewModel(private val mainView: MainView, application: Application) : AndroidViewModel
    (application), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private val speechRecognizer by lazy {
        SpeechRecognizer()
    }


    private val speechListener = object : SpeechRecognizer.SpeechListener {


        override fun onCompleted(recognizedText: String) {

            recognizedTextLiveData.postValue("")
            launch {
                val response =
                    handsFreeApi.postMainAsync(MainBody("test", recognizedText, locale).toRequestBody()).await()


                val messageIcon = when {
                    response.isCorrect == true -> SpeechItem.MessageIcon(R.drawable.logo_correct)
                    response.isCorrect == false -> SpeechItem.MessageIcon(R.drawable.logo_retry)
                    else -> null
                }
                mainView.addTTSBubble(SpeechItem(recognizedText, SpeechType.SENT, messageIcon))
                handleResponse(response)
            }
        }

        override fun onSpeechRecognized(speechResponse: SpeechResponse) {
            SpeechRecognizer.recognizedText = speechResponse.speechResponseAsText
            recognizedTextLiveData.postValue(speechResponse.speechResponseAsText)
            if (speechResponse.isFinal) stopSpeechListening()
        }

        override fun onBind() {
            audioPlayer.audioPlayerListener = object : AudioPlayer.AudioPlayerListener {
                override fun onAudioCompleted(audioId: Int) {
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

    }

    private val locale: String = ""


    private fun emptyRequest() {
        launch {
            val response = handsFreeApi.postMainAsync(MainBody("test", location = locale).toRequestBody())
                .await()
            handleResponse(response)
        }
    }

    val recognizedTextLiveData by lazy {
        MutableLiveData<String>()
    }

    private val audioPlayer by lazy {
        AudioPlayer(application)
    }
    private val handsFreeApi by lazy {
        HandsfreeClient.client
    }


    private var cachedResponse: ResponseFromMainAPi? = null


    fun startSpeechListening() {
        mainView.showMicInput()
        audioPlayer.play(R.raw.speak, -1)

        speechRecognizer.startVoiceRecorder(
            cachedResponse?.inputLang ?: "",
            true,
            cachedResponse?.inputHints ?: emptyList()
        )
    }

    fun stopSpeechListening() {

        speechRecognizer.stopVoiceRecorder()
        mainView.hideMicInput()

    }


    fun playOutPuts() {
        val audioId =
            if (cachedResponse?.hasInput == true) AudioPlayer.AUDIO_ID_START_RECOGNITION else AudioPlayer.AUDIO_ID_NEW_REQUEST

        audioPlayer.play(cachedResponse!!.output, audioId)
    }


    fun playAudioFeedBack(isCorrect: Boolean, audioId: Int) =
        if (isCorrect) audioPlayer.play(R.raw.fast_correct, audioId)
        else audioPlayer.play(R.raw.fast_incorrect, audioId)


    fun bind(application: Application) {
        speechRecognizer.bindService(application, speechListener)
    }

    private fun handleResponse(response: ResponseFromMainAPi) {

        cachedResponse = response

        if (response.isCorrect != null) {
            playAudioFeedBack(response.isCorrect, AudioPlayer.AUDIO_ID_PLAY_OUTPUTS)
        } else {
            playOutPuts()
        }
    }

    fun start() {
        launch {
            handsFreeApi.initAsync().await()
            val response = handsFreeApi.postMainAsync(MainBody("test", location = locale).toRequestBody()).await()
            handleResponse(response)
        }
    }
}