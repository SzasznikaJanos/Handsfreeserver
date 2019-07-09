package com.example.handsfree_server

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log

import com.example.handsfree_server.api.MainBody
import com.example.handsfree_server.api.ReadbackBody
import com.example.handsfree_server.model.AudioPlayer
import com.example.handsfree_server.model.SpeechItem
import com.example.handsfree_server.model.SpeechType
import com.example.handsfree_server.pojo.InitData
import com.example.handsfree_server.pojo.ReadBackResponse
import com.example.handsfree_server.pojo.ResponseFromMainAPi
import com.example.handsfree_server.repository.HandsFreeRepository
import com.example.handsfree_server.speechrecognizer.Recognizer


import com.example.handsfree_server.util.ServerResult

import com.example.handsfree_server.view.MainView

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class CustomService : Service(), CoroutineScope {


    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private var sessionId = ""

    private val locale: String  by lazy { java.util.TimeZone.getDefault().id }
    private var cachedResponse: ResponseFromMainAPi? = null
    private var audioPlayer: AudioPlayer? = null
    private var recognizer: Recognizer? = null

    val hint
        get() = cachedResponse?.inputHints?.firstOrNull()

    private val repository by lazy {
        HandsFreeRepository.getInstance()
    }


    private val initData by lazy {
        InitData(
            "android-0", 2, 1, 2, listOf(
                10027,
                10026,
                10032,
                10033,
                6613,
                7179,
                7178,
                7177,
                7176
            ),
            locale
        )
    }
    private lateinit var mainView: MainView


    companion object {
        fun fromBinder(
            mainVIew: MainView,
            binder: IBinder,
            audioPlayer: AudioPlayer,
            recognizer: Recognizer
        ): CustomService = (binder as CustomBinder).service.apply {

            this.mainView = mainVIew
            this.audioPlayer = audioPlayer
            this.recognizer = recognizer


            audioPlayer.audioPlayerListener = object : AudioPlayer.AudioPlayerListener {
                override fun onAudioCompleted(audioId: Int) {
                    when (audioId) {
                        AudioPlayer.AUDIO_ID_START_RECOGNITION -> startRecognizing()
                        AudioPlayer.AUDIO_ID_PLAY_FEEDBACK -> playAudioFeedBack(
                            cachedResponse!!.answerStatus?.isCorrect!!,
                            AudioPlayer.AUDIO_ID_PLAY_OUTPUTS
                        )
                        AudioPlayer.AUDIO_ID_PLAY_OUTPUTS -> playOutPuts()
                        AudioPlayer.AUDIO_ID_NEW_REQUEST -> emptyRequest()
                        AudioPlayer.AUDIO_ID_SHOW_DIALOG -> mainView.showDialog(cachedResponse!!.dialogType!!)
                        AudioPlayer.AUDIO_ID_READBACK -> handleResponse(cachedResponse!!)
                    }
                }

                override fun onAudioStart(speechItem: SpeechItem) = mainView.addTTSBubble(speechItem)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return CustomBinder()
    }

    inner class CustomBinder : Binder() {
        val service: CustomService
            get() = this@CustomService
    }


    fun startRecognizing() {
        mainView.showMicInput()
        mainView.showButtons(listOf("help", "pause", "skip", "cancelRecognition"))
        recognizer?.startListening(cachedResponse!!.inputLang)
    }

    fun cancelRecognition() = recognizer?.cancelRecognition()

    fun stopRecognition() = recognizer?.stopRecognition()


    private fun playReadBack(response: ReadBackResponse) =
        audioPlayer?.playReadback(response.audioLink, AudioPlayer.AUDIO_ID_READBACK)


    fun handleReadBack(userResponse: String) {
        launch {
            safeResponse(repository.sendResponseToServer(MainBody(sessionId, userResponse, locale))) {

                val answerStatus = it.answerStatus
                val readBackTextRequest = if (answerStatus != null && answerStatus.isCorrect)
                    answerStatus.correctAnswer else userResponse

                val isCorrect = answerStatus?.isCorrect

                launch {
                    safeResponse(
                        repository.getReadBackMessage(
                            ReadbackBody(cachedResponse!!.inputLang, userResponse)
                        )
                    ) { readbackResponse ->
                        val messageIcon = isCorrect?.let { getResponseSpeechItem(isCorrect) }
                        Log.d("Test", "handleReadBack: adding bubble text:$readBackTextRequest")
                        mainView.addTTSBubble(SpeechItem(userResponse, SpeechType.SENT, messageIcon))
                        cachedResponse = it
                        playReadBack(readbackResponse)
                    }
                }
            }
        }
    }


    private fun handleResponse(response: ResponseFromMainAPi) {

        if (!response.output.isNullOrEmpty()) {
            val withOption = response.output.find { !it.options.isNullOrEmpty() }
            if (withOption != null) {
                mainView.showTopicRecyclerView(withOption.options)
            }
        }


        if (response.answerStatus?.isCorrect != null) {
            playAudioFeedBack(response.answerStatus.isCorrect, AudioPlayer.AUDIO_ID_PLAY_OUTPUTS)
        } else {
            playOutPuts()
        }

    }

    fun playAudioFeedBack(isCorrect: Boolean, audioId: Int) =
        if (isCorrect) audioPlayer?.play(R.raw.fast_correct, audioId)
        else audioPlayer?.play(R.raw.fast_incorrect, audioId)

    fun playOutPuts() = audioPlayer?.play(cachedResponse!!.output, getAudioAction())


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


    fun emptyRequest() = launch {
        safeResponse(repository.sendResponseToServer(MainBody(sessionId, location = locale))) {
            cachedResponse = it
            handleResponse(it)
        }
    }


    private fun <T> safeResponse(result: ServerResult<T>, onSuccess: (data: T) -> Unit) {
        when (result) {
            is ServerResult.Success -> onSuccess(result.data)
            is ServerResult.Error -> mainView.showErrorMessage(result.exception.displayMessage)
        }
    }

    fun start() {
        launch {
            safeResponse(repository.initServerList(initData)) {
                sessionId = it.sessionId

                launch {
                    val newResponse = repository.sendResponseToServer(MainBody(sessionId, location = locale))

                    safeResponse(newResponse) {
                        cachedResponse = it
                        handleResponse(it)
                    }
                }
            }


        }
    }


    private fun getResponseSpeechItem(isCorrect: Boolean): SpeechItem.MessageIcon? {
        return when {
            isCorrect -> SpeechItem.MessageIcon(R.drawable.logo_correct)
            else -> SpeechItem.MessageIcon(R.drawable.logo_retry)

        } //SpeechItem(recognizedText, SpeechType.SENT, messageIcon)
    }

    fun handleFallBack() {
        launch {
            mainView.hideMicInput()
            safeResponse(repository.getFallBackMessage(sessionId)) {
                audioPlayer?.playReadback(it.audioLink, AudioPlayer.AUDIO_ID_START_RECOGNITION)
            }
        }
    }

    fun handleTimeOut() {
        launch {
            mainView.hideMicInput()
            safeResponse(repository.getTimeOutMessage(sessionId)) {
                if (it.pause) {
                    mainView.pause()
                } else {
                    audioPlayer?.playReadback(it.audio, AudioPlayer.AUDIO_ID_START_RECOGNITION)
                }
            }
        }
    }

    fun stopAudio() {
        audioPlayer?.stop()
    }
}