package com.example.handsfree_server

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color

import android.media.AudioManager
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.system.Os.bind

import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.handsfree_server.adapters.Adapter
import com.example.handsfree_server.adapters.TopicAdapter
import com.example.handsfree_server.model.AudioPlayer

import com.example.handsfree_server.model.SpeechItem
import com.example.handsfree_server.presenter.Presenter
import com.example.handsfree_server.speechrecognizer.Recognizer

import com.example.handsfree_server.view.MainView
import com.google.android.material.snackbar.Snackbar
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_buttons.*
import kotlinx.coroutines.*
import java.util.*


import kotlin.coroutines.CoroutineContext


class MainActivity : AppCompatActivity(), CoroutineScope, MainView, RecognitionListener, ServiceConnection,
    View.OnClickListener,
    AudioManager.OnAudioFocusChangeListener {


    private var customService: CustomService? = null

    private val audioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private var _dialog: PopupDialog? = null

    private val adapter by lazy { Adapter() }

    private val layoutManager by lazy {
        LinearLayoutManager(this, RecyclerView.VERTICAL, false)
    }

    private val topicAdapter by lazy {
        TopicAdapter {
            customService?.stopRecognizing()
            presenter.handleReadBack(it)
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
    }

    private var targetAudioLevel: Int = 0

    private val volumeObserver by lazy {
        VolumeObserver(this, Handler(Looper.getMainLooper()), object : OnVolumeChangeListener {
            override fun onVolumeChanged(newValue: Int) {
                targetAudioLevel = newValue
            }
        })
    }

    private val presenter: Presenter by lazy {
        Presenter(this)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainRecycler.layoutManager = layoutManager
        mainRecycler.adapter = adapter
        //itemAnimator

        topicsRecyclerView.adapter = topicAdapter


        checkPermissions()

        presenter.recognizedTextLiveData.observe(this, androidx.lifecycle.Observer {
            speechRecognized_textView.text = it
        })

        targetAudioLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        applicationContext.contentResolver.registerContentObserver(
            android.provider.Settings.System.CONTENT_URI,
            true, volumeObserver
        )

        pauseButton.setOnClickListener {
            if (pauseButton.text.toString() == "Pause") {
                customService?.stopRecognizing()
                customService?.stopAudio()
                hideTopicRecyclerView()
                hideMicInput()

                pauseButton.text = "Resume"
            } else {
                presenter.emptyRequest()
                pauseButton.text = "Pause"
            }
        }

        button_one.setOnClickListener(this)
        button_two.setOnClickListener(this)
        button_three.setOnClickListener(this)
        button_four.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.button_one, R.id.button_two, R.id.button_three, R.id.button_four -> handleButtonEvent((v as Button).text.toString())
        }
    }

    private fun handleButtonEvent(text: String) {
        customService?.stopRecognizing()
        customService?.stopAudio()
        hideTopicRecyclerView()
        hideMicInput()
        presenter.handleReadBack(text)
    }

    override fun showErrorMessage(errorMessage: String) {
        runOnUiThread {
            hideTopicRecyclerView()
            hideMicInput()
            mainRecycler.visibility = View.INVISIBLE
            error_textView.visibility = View.VISIBLE
            error_textView.text = errorMessage
        }
    }

    override fun showTopicRecyclerView(topics: List<String>) {
        topicAdapter.setTopics(topics)
        topicsRecyclerView.visibility = View.VISIBLE
    }

    override fun hideTopicRecyclerView() {
        topicsRecyclerView.visibility = View.INVISIBLE
    }

    override fun hideErrorMessage() {

    }

    override fun updateQuizResult(speechItem: SpeechItem.MessageIcon?) = runOnUiThread {
        adapter.updateResultIcon(speechItem)
    }

    override fun showButtons(buttonTexts: List<String>) = runOnUiThread {
        return@runOnUiThread
        buttonsLayout.visibility = View.VISIBLE
        for (i in 0..4) {
            val buttonText = buttonTexts.elementAtOrNull(i)
            val button = when (i) {
                0 -> button_one
                1 -> button_two
                2 -> button_three
                else -> button_four
            }
            setButtonTextAndVisibility(button, buttonText)
        }
    }

    private fun setButtonTextAndVisibility(button: Button, text: String?) {
        button.visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
        button.text = text
    }

    override fun hideButtons() = runOnUiThread {
        return@runOnUiThread
        buttonsLayout.visibility = View.GONE
    }

    override fun showDialog(dialogType: String) {
        when (dialogType) {
            "stop" -> createAndShowDialog("Stopped", "Start") {
                it.dismiss()
                presenter.emptyRequest()
            }
        }
    }

    override fun hideMicInput() = runOnUiThread {
        mic_input_imageView.visibility = View.INVISIBLE
        hideButtons()
    }

    override fun showMicInput() = runOnUiThread {
        mic_input_imageView.visibility = View.VISIBLE
    }

    override fun addTTSBubble(speechItem: SpeechItem) = addItem(speechItem)

    private fun addItem(speechItem: SpeechItem) {
        runOnUiThread {
            adapter.addItem(speechItem)
            scrollToPositionIfNeed(false)
        }
    }

    private fun createAndShowDialog(
        messageText: String,
        buttonText: String,
        onCancelCallBack: (dialog: PopupDialog) -> Unit
    ) {
        runOnUiThread {
            if (_dialog?.isShowing() == true) _dialog?.dismiss()
            PopupDialog().apply {
                message = messageText
                this.buttonText = buttonText
                onDismissCallBack = { onCancelCallBack(this) }
                _dialog = this
            }.show(supportFragmentManager, "Dialog")
        }
    }

    private fun scrollToPositionIfNeed(scrollImmediately: Boolean) {
        val positionToScroll = if (adapter.itemCount < 1) return else adapter.itemCount - 1

        try {
            if (scrollImmediately) {
                mainRecycler.smoothScrollToPosition(positionToScroll)
            } else {
                if (hasWindowFocus() && layoutManager.findLastVisibleItemPosition() >= positionToScroll - 1) {
                    layoutManager.scrollToPosition(positionToScroll)
                }
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }


    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main


    private fun checkPermissions() {
        val rationalString = "We need Voice recorder permission to use speech recognition."

        val permissionHandler = object : PermissionHandler() {
            override fun onGranted() {
                bind()
            }

            override fun onDenied(context: Context?, deniedPermissions: ArrayList<String>?) =
                handleDeniedPermission("We need Audio Record permission so we can use Speech Recognition")

            override fun onBlocked(context: Context?, blockedList: ArrayList<String>?): Boolean {
                handleDeniedPermission("Please grand Audio Record permissions  from settings in order to use speech recognition")
                return super.onBlocked(context, blockedList)
            }

        }

        Permissions.Options()
        val x = arrayOf(android.Manifest.permission.RECORD_AUDIO)
        Permissions.check(
            this,
            x,
            rationalString,
            Permissions.Options(),
            permissionHandler
        )
    }

    private fun handleDeniedPermission(message: String) {
        val snackBar = Snackbar.make(this.window.decorView, message, Snackbar.LENGTH_INDEFINITE)
        snackBar.setActionTextColor(Color.YELLOW)
        snackBar.setAction("OK") { checkPermissions() }
        snackBar.show()
    }

    override fun onResume() {
        super.onResume()
        scrollToPositionIfNeed(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbind()
    }

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d("Recognizer", "onReadyForSpeech: ")


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
                presenter.recognizedTextLiveData.postValue(data.last())
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        Log.d("Recognizer", "onEvent: $eventType")
    }

    override fun onBeginningOfSpeech() {
        Log.d("Recognizer", "onBeginningOfSpeech: ")

    }

    override fun onEndOfSpeech() {
        Log.d("Recognizer", "onEndOfSpeech: ")
    }

    override fun onError(error: Int) {
        Log.d("Recognizer", "onError: $error")
        presenter.recognizedTextLiveData.value = ""

        when (error) {
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> customService?.handleTimeOut()
            SpeechRecognizer.ERROR_NO_MATCH -> customService?.handleFallBack()
        }

    }

    private fun getRecognizedText(results: List<String>, targetText: String) =
        results.find { it.toLowerCase() == targetText.toLowerCase() } ?: results[0]

    override fun onResults(results: Bundle?) {

        if (results != null && results.containsKey(SpeechRecognizer.RESULTS_RECOGNITION)) {
            val data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (data != null) {
                val recognizedText = customService?.hint?.let {
                    getRecognizedText(data, it)
                } ?: data[0]

                Log.d(
                    "Recognizer",
                    "onResults: $data \n target: ${customService?.hint} \n recognizedText =$recognizedText"
                )


                presenter.handleReadBack(recognizedText)
            }
        }
        hideTopicRecyclerView()
        hideMicInput()
    }

    fun bind() {
        bindService(Intent(this, CustomService::class.java), this, Context.BIND_AUTO_CREATE)
    }

    fun unbind() {
        unbindService(this)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        service?.let {
            customService = CustomService.fromBinder(this, service, AudioPlayer(this), Recognizer(this, this)).also {
                presenter.service = it
            }
        }
        presenter.start()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.d("Service", "onServiceDisconnected: ${name?.shortClassName}")
    }

    override fun onAudioFocusChange(focusChange: Int) {
        Log.d("Focus changed", "onAudioFocusChange: $focusChange")
    }


}
