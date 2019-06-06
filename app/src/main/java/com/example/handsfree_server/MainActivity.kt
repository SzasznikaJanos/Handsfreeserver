package com.example.handsfree_server

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.util.Log


import android.view.View

import androidx.lifecycle.ViewModelProviders

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.handsfree_server.adapters.Adapter

import com.example.handsfree_server.model.SpeechItem
import com.example.handsfree_server.speechrecognizer.Recognizer

import com.example.handsfree_server.view.MainView
import com.google.android.material.snackbar.Snackbar
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.util.*


import kotlin.coroutines.CoroutineContext


class MainActivity : AppCompatActivity(), CoroutineScope, MainView, RecognitionListener {

    private val audioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    private var _dialog: PopupDialog? = null
    private val adapter by lazy { Adapter() }

    private val layoutManager by lazy {
        LinearLayoutManager(this, RecyclerView.VERTICAL, false)
    }

    private var targetAudioLevel: Int = 0

    private val volumeObserver by lazy {
        VolumeObserver(this, Handler(Looper.getMainLooper()), object : OnVolumeChangeListener {
            override fun onVolumeChanged(newValue: Int) {
                targetAudioLevel = newValue
            }
        })
    }

    private val viewModel: MainViewModel by lazy {
        ViewModelProviders.of(
            this@MainActivity,
            MainViewModelFactory(this@MainActivity, this@MainActivity, this@MainActivity.application)
        )[MainViewModel::class.java]
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainRecycler.layoutManager = layoutManager
        mainRecycler.adapter = adapter
        checkPermissions()

        viewModel.recognizedTextLiveData.observe(this, androidx.lifecycle.Observer {
            speechRecognized_textView.text = it
        })

        localTestButton.setOnClickListener {
            viewModel.startRecognizer()
            localTestButton.visibility = View.INVISIBLE
            cloudTestButton.visibility = View.INVISIBLE
        }

        targetAudioLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        applicationContext.contentResolver.registerContentObserver(
            android.provider.Settings.System.CONTENT_URI,
            true, volumeObserver
        )


    }


    override fun updateQuizResult(resultIcon: SpeechItem.MessageIcon?) = runOnUiThread {
        adapter.updateResultIcon(resultIcon)
    }

    override fun showRecogButtons() {
        runOnUiThread {
            localTestButton.visibility = View.VISIBLE
            cloudTestButton.visibility = View.VISIBLE
        }
    }

    override fun hideButtons() {
        runOnUiThread {
            localTestButton.visibility = View.INVISIBLE
            cloudTestButton.visibility = View.INVISIBLE
        }
    }

    override fun showDialog(dialogType: String) {
        when (dialogType) {
            "stop" -> createAndShowDialog("Stopped", "Start") {
                it.dismiss()
                viewModel.emptyRequest()
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
                viewModel.start()
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
                viewModel.recognizedTextLiveData.postValue(data.last())
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        Log.d("Recognizer", "onEvent: $eventType")
    }

    override fun onBeginningOfSpeech() {
        Log.d("Recognizer", "onBeginningOfSpeech: ")
        unMute()
    }

    override fun onEndOfSpeech() {
        Log.d("Recognizer", "onEndOfSpeech: ")
    }

    override fun onError(error: Int) {
        Log.d("Recognizer", "onError: $error")
        when (error) {
            6 -> {
                viewModel.retryListening()
            }
            7 -> handleSound()
        }

    }

    override fun onResults(results: Bundle?) {
        unMute()
        if (results != null && results.containsKey(SpeechRecognizer.RESULTS_RECOGNITION)) {
            val data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (data != null) {
                Log.d("Recognizer", "onResults: $data")
                val recognizedText = data.first()
                viewModel.recognizedTextLiveData.postValue("")
                com.example.handsfree_server.speechrecognizer.SpeechRecognizer.recognizedText = recognizedText
                viewModel.handleReadBack()
            }
        }
        hideMicInput()
    }

    private fun handleSound() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        Log.d(
            "audioManager",
            "handle sound before mute level:${audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)}"
        )
        mute()
        viewModel.startRecognizer()
    }

    fun mute() {
        volumeObserver.withListening = false
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
    }

    fun unMute() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        Log.d("audioManager", "unMute: level:$targetAudioLevel")
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetAudioLevel, 0)
        volumeObserver.withListening = true
        Log.d(
            "audioManager",
            "unMute: Stream level after umute: ${audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)}"
        )

    }
}
