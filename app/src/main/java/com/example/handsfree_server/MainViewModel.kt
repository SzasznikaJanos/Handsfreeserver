package com.example.handsfree_server

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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


class MainViewModel(context: Context, private val mainView: MainView) : ViewModel(),
    RecognitionListener, CoroutineScope {


    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private var sessionId = ""

    private val locale: String  by lazy { java.util.TimeZone.getDefault().id }


    private var cachedResponse: ResponseFromMainAPi? = null

    private val audioPlayer: AudioPlayer by lazy {
        AudioPlayer(context)
    }

    private val recognizer by lazy {
        Recognizer(context, this)
    }

    private val repository by lazy {
        HandsFreeRepository.getInstance()
    }

    private val hint
        get() = cachedResponse?.inputHints?.firstOrNull()


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

    val recognitionLiveData by lazy {
        MutableLiveData<String>()
    }


    init {
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

    private fun getRecognizedText(results: List<String>, targetText: String, withDefaultValue: Boolean = true) =
        results.find { it.toLowerCase() == targetText.toLowerCase() } ?: if (withDefaultValue) results[0] else null


    override fun onReadyForSpeech(params: Bundle?) {

    }

    override fun onRmsChanged(rmsdB: Float) {

    }

    override fun onBufferReceived(buffer: ByteArray?) {

    }

    override fun onPartialResults(partialResults: Bundle?) {
        if (partialResults != null && partialResults.containsKey(SpeechRecognizer.RESULTS_RECOGNITION)) {
            val data = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

            if (data != null) {
                Log.d("Recognizer", "onPartialResults: $partialResults")


                val recognizedText = hint?.let {
                    getRecognizedText(data, it, false)
                }
                Log.d("Test", "onPartialResults: target text:$recognizedText")



                if (!recognizedText.isNullOrBlank()) {
                    stopRecognition()
                }

                recognitionLiveData.postValue(data.last())
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {

    }

    override fun onBeginningOfSpeech() {

    }

    override fun onEndOfSpeech() {

    }

    override fun onError(error: Int) {
        Log.d("Recognizer", "onError: $error")
        recognitionLiveData.value = ""

        when (error) {
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
            SpeechRecognizer.ERROR_NO_MATCH -> handleTimeOut()
            // customService?.handleFallBack()
        }
    }

    private fun handleTimeOut() {
        launch {
            mainView.hideMicInput()
            safeResponse(repository.getTimeOutMessage(sessionId)) {
                if (it.pause) {
                    mainView.pause()
                } else {
                    audioPlayer.playReadback(it.audio, AudioPlayer.AUDIO_ID_START_RECOGNITION)
                }
            }
        }
    }

    override fun onResults(results: Bundle?) {
        if (results != null && results.containsKey(SpeechRecognizer.RESULTS_RECOGNITION)) {
            val data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (data != null) {
                val recognizedText = hint?.let {
                    getRecognizedText(data, it)
                } ?: data[0]
                handleReadBack(recognizedText)
            }
        }
    }

    fun startRecognizing() {
        mainView.showMicInput()
        mainView.showButtons(listOf("help", "pause", "skip", "cancelRecognition"))
        recognizer.startListening(cachedResponse!!.inputLang)
    }

    fun cancelRecognition() = recognizer.cancelRecognition()

    fun stopRecognition() = recognizer.stopRecognition()

    fun handleReadBack(recognizedText: String?) {
        recognitionLiveData.postValue("")
        mainView.hideButtons()
        mainView.hideTopicRecyclerView()
        mainView.hideMicInput()

            launch {
                safeResponse(repository.sendResponseToServer(MainBody(sessionId, recognizedText, locale))) {

                    val answerStatus = it.answerStatus
                    val readBackTextRequest = if (answerStatus != null && answerStatus.isCorrect)
                        answerStatus.correctAnswer else recognizedText

                    val isCorrect = answerStatus?.isCorrect

                    launch {
                        safeResponse(
                            repository.getReadBackMessage(
                                ReadbackBody(cachedResponse!!.inputLang, readBackTextRequest)
                            )
                        ) { readbackResponse ->
                            val messageIcon = isCorrect?.let { getResponseSpeechItem(isCorrect) }
                            Log.d("Test", "handleReadBack: adding bubble text:$readBackTextRequest")
                            mainView.addTTSBubble(SpeechItem(readBackTextRequest, SpeechType.SENT, messageIcon))
                            cachedResponse = it
                            playReadBack(readbackResponse)
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

    private fun playReadBack(response: ReadBackResponse) =
        audioPlayer.playReadback(response.audioLink, AudioPlayer.AUDIO_ID_READBACK)


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


    fun stopAudio() {
        audioPlayer.stop()
    }


    private fun <T> safeResponse(result: ServerResult<T>, onSuccess: (data: T) -> Unit) {
        when (result) {
            is ServerResult.Success -> onSuccess(result.data)
            is ServerResult.Error -> mainView.showErrorMessage(result.exception.displayMessage)
        }
    }
}
