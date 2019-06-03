package com.example.handsfree_server

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel

import androidx.lifecycle.MutableLiveData
import com.example.handsfree_server.api.HandsFreeClient
import com.example.handsfree_server.api.MainBody
import com.example.handsfree_server.model.AudioPlayer
import com.example.handsfree_server.model.SpeechItem
import com.example.handsfree_server.model.SpeechType
import com.example.handsfree_server.pojo.ResponseFromMainAPi
import com.example.handsfree_server.speechrecognizer.SpeechRecognizer
import com.example.handsfree_server.speechrecognizer.SpeechResponse
import com.example.handsfree_server.util.toRequestBody
import com.example.handsfree_server.view.MainView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class MainViewModel(private val mainView: MainView, application: Application) : AndroidViewModel(application),
    CoroutineScope {

    private var job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val speechRecognizer by lazy { SpeechRecognizer() }

    private val locale: String  by lazy { java.util.TimeZone.getDefault().id }

    private var isFinishing = false


    val recognizedTextLiveData by lazy { MutableLiveData<String>() }

    private val audioPlayer by lazy { AudioPlayer(application) }

    private val handsFreeApi by lazy { HandsFreeClient.client }

    private var cachedResponse: ResponseFromMainAPi? = null

    private val speechListener = object : SpeechRecognizer.SpeechListener {


        override fun onCompleted(recognizedText: String) {

            stopSpeechListening()
            recognizedTextLiveData.postValue("")
            CoroutineScope(Dispatchers.Main).launch {

                val response =
                    handsFreeApi.postMainAsync(MainBody("test", recognizedText, locale).toRequestBody()).await()
                mainView.addTTSBubble(getResponseSpeechItem(response, recognizedText))
                handleResponse(response)
            }
        }

        override fun onSpeechRecognized(speechResponse: SpeechResponse) {
            if (isFinishing) return
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
                        AudioPlayer.AUDIO_ID_SHOW_DIALOG -> showDialog()
                    }
                }

                override fun onAudioStart(speechItem: SpeechItem) = mainView.addTTSBubble(speechItem)
            }
            start()
        }

    }

    private fun getResponseSpeechItem(response: ResponseFromMainAPi, recognizedText: String): SpeechItem {
        val messageIcon = when {
            response.isCorrect == true -> SpeechItem.MessageIcon(R.drawable.logo_correct)
            response.isCorrect == false -> SpeechItem.MessageIcon(R.drawable.logo_retry)
            else -> null
        }
        return SpeechItem(recognizedText, SpeechType.SENT, messageIcon)
    }


    fun showDialog() {
        cachedResponse?.let {
            if (it.dialogType != null) mainView.showDialog(it.dialogType)
        }
    }

    fun emptyRequest() {
        launch {
            val response = handsFreeApi.postMainAsync(MainBody("test", location = locale).toRequestBody())
                .await()
            handleResponse(response)
        }
    }


    fun startSpeechListening() {

        isFinishing = false
        mainView.showMicInput()
        audioPlayer.play(R.raw.speak, -1)

        speechRecognizer.startVoiceRecorder(
            cachedResponse?.inputLang ?: "",
            true,
            cachedResponse?.inputHints ?: emptyList()
        )

        cachedResponse = null
    }

    fun stopSpeechListening() {
        isFinishing = true
        speechRecognizer.stopVoiceRecorder()
        mainView.hideMicInput()

    }


    fun playOutPuts() = audioPlayer.play(cachedResponse!!.output, getAudioAction())

    private fun getAudioAction(): Int {
        return cachedResponse?.let { response ->
            return when {
                !response.dialogType.isNullOrBlank() -> AudioPlayer.AUDIO_ID_SHOW_DIALOG
                response.hasInput -> AudioPlayer.AUDIO_ID_START_RECOGNITION
                !response.hasInput -> AudioPlayer.AUDIO_ID_NEW_REQUEST

                else -> throw IllegalAccessException("audio action case not implemented")
            }
        } ?: throw IllegalAccessException("Cached response is null cannot get the audio id")
    }


    fun playAudioFeedBack(isCorrect: Boolean, audioId: Int) =
        if (isCorrect) audioPlayer.play(R.raw.fast_correct, audioId)
        else audioPlayer.play(R.raw.fast_incorrect, audioId)


    fun bind(application: Application) = speechRecognizer.bindService(application, speechListener)

    private fun handleResponse(response: ResponseFromMainAPi) {
        cachedResponse = response

        Log.d("API", "handleResponse: $response")
        if (response.isCorrect != null) {
            playAudioFeedBack(response.isCorrect, AudioPlayer.AUDIO_ID_PLAY_OUTPUTS)
        } else {
            playOutPuts()
        }

        isFinishing = true
    }

    fun start() {
        launch {
            handsFreeApi.initAsync().await()
            val response = handsFreeApi.postMainAsync(MainBody("test", location = locale).toRequestBody()).await()
            handleResponse(response)
        }
    }
}