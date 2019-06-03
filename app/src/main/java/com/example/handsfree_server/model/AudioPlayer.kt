package com.example.handsfree_server.model

import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import androidx.annotation.RawRes
import androidx.core.net.toUri
import com.example.handsfree_server.pojo.Output
import com.example.handsfree_server.util.TAG
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class AudioPlayer(private val context: Context) : CoroutineScope {


    companion object {

        const val AUDIO_ID_START_RECOGNITION = 0
        const val AUDIO_ID_PLAY_FEEDBACK = 1
        const val AUDIO_ID_PLAY_OUTPUTS = 2
        const val AUDIO_ID_NEW_REQUEST = 3
        const val AUDIO_ID_SHOW_DIALOG = 4
    }


    private var job = SupervisorJob()


    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job


    interface AudioPlayerListener {
        fun onAudioCompleted(audioId: Int)
        fun onAudioStart(speechItem: SpeechItem)
    }

    private var audioId = 0

    var audioPlayerListener: AudioPlayerListener? = null

    private val mediaPlayer = MediaPlayer()


    private var outputs: List<Output> = mutableListOf()
    private var outPutPosition = 0


    init {
        mediaPlayer.setOnCompletionListener {

            if (outPutPosition in 0 until outputs.size) {
                handleConsecutivePlay()
            } else {
                audioPlayerListener?.onAudioCompleted(audioId)
            }

        }

        mediaPlayer.setOnPreparedListener { player ->
            try {
                buildSpeechItem()?.let {
                    audioPlayerListener?.onAudioStart(it)
                }
                player.start()
            } catch (ex: Exception) {
                Log.e(TAG, "playAudio: ", ex)
            }
        }
    }


    private fun prepareMediaPlayerForStart(uri: Uri, isLocal: Boolean = false) {
        job = Job()
        launch {
            stop()
            try {
                mediaPlayer.reset()
                if (isLocal) {
                    mediaPlayer.setDataSource(context, uri)
                } else {
                    val uriString = uri.toString()
                    mediaPlayer.setDataSource(uriString)
                }
                mediaPlayer.prepareAsync()
            } catch (exception: Exception) {
                exception.printStackTrace()
                audioPlayerListener?.onAudioCompleted(audioId)
            }
        }
    }

    private fun getUriFromResId(@RawRes resourceId: Int): Uri {
        val resourcePath = "android.resource://${context.packageName}/" + resourceId
        return Uri.parse(resourcePath)
    }

    fun play(@RawRes resId: Int, audioId: Int) {
        this.audioId = audioId
        prepareMediaPlayerForStart(getUriFromResId(resId), isLocal = true)
    }

    fun play(outputs: List<Output>, audioId: Int) {
        Log.d("AudioPlayer", "playing with action id: $audioId ")
        this.outputs = outputs
        this.audioId = audioId
        outPutPosition = 0

        prepareMediaPlayerForStart(outputs.first().audio.toUri())
    }


    private fun handleConsecutivePlay() {
        outPutPosition++
        if (outPutPosition in 0 until outputs.size) {
            prepareMediaPlayerForStart(outputs[outPutPosition].audio.toUri())
        } else if (outPutPosition == outputs.size) {
            audioPlayerListener?.onAudioCompleted(audioId)
        }
    }

    fun stop() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        try {
            job.cancel()
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
        mediaPlayer.reset()
    }


    private fun buildSpeechItem(): SpeechItem? {
        if (outPutPosition in 0 until outputs.size) {
            val output = outputs[outPutPosition]
            return if (output.type == "phrase") {
                buildSpeechItemSpannableFromQuizItem(output) ?: SpeechItem(output.text)
            } else {
                SpeechItem(output.text)
            }
        }
        return null
    }


    private fun buildSpeechItemSpannableFromQuizItem(outPut: Output): SpeechItem? {
        val spannableText = transformTextIntoSpannable(outPut.text)
        return SpeechItem(spannableText, iconImageResId = SpeechItem.MessageIcon(getFlagResIdFromOutPut(outPut), true))
    }

    private fun transformTextIntoSpannable(text: String): SpannableString {

        val spannableString = SpannableString(text)
        spannableString.setSpan(ForegroundColorSpan(Color.YELLOW), 0, text.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        return spannableString
    }

    private fun getFlagResIdFromOutPut(outPut: Output): Int {
        try {
            val resName = "flag_${outPut.language}"
            return context.resources.getIdentifier(resName, "drawable", context.packageName)
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
        return -1
    }
}