package com.example.handsfree_server.model

import android.content.Context
import android.graphics.Color

import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.annotation.IdRes
import androidx.annotation.RawRes
import com.example.handsfree_server.R

import com.example.handsfree_server.pojo.Output

import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player

import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.upstream.*

import com.google.android.exoplayer2.util.Util


class AudioPlayer(val context: Context) {

    companion object {

        const val AUDIO_ID_START_RECOGNITION = 0
        const val AUDIO_ID_PLAY_FEEDBACK = 1
        const val AUDIO_ID_PLAY_OUTPUTS  =2
        const val AUDIO_ID_NEW_REQUEST  = 3
    }

    interface AudioPlayerCallbacks {
        fun onAudioEnd(audioId: Int)
        fun onAudioStart(speechItem: SpeechItem)
    }

    private val exoPlayer by lazy { ExoPlayerFactory.newSimpleInstance(context) }
    private val userAgent = Util.getUserAgent(context, "audioPlayer")
    private val dataSourceFactory = DefaultDataSourceFactory(context, userAgent, DefaultBandwidthMeter())
    private var outputs: List<Output> = mutableListOf()
    private var outPutPosition = 0
    private var audioId = -1

    private val audioListener = object : Player.EventListener {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    if(outPutPosition in 0 until outputs.size){
                        handleConsecutivePlay()
                    }else {
                        audioPlayerCallBack?.onAudioEnd(audioId)
                    }
                }
                Player.STATE_READY -> buildSpeechItem()?.let { audioPlayerCallBack?.onAudioStart(it) }
            }
        }
    }

    var audioPlayerCallBack: AudioPlayerCallbacks? = null


    init {
        exoPlayer.addListener(audioListener)
    }



    private fun handleConsecutivePlay() {
        outPutPosition++
        if (outPutPosition in 0 until outputs.size) {
            prepareAudio(outputs[outPutPosition].audio)
        } else if (outPutPosition == outputs.size) {
            audioPlayerCallBack?.onAudioEnd(audioId)
        }
    }

    private fun buildSpeechItemSpannableFromQuizItem(outPut: Output): SpeechItem? {
        val spannableText = transformTextIntoSpannable(outPut.text)
        return SpeechItem(spannableText, iconImageResId = SpeechItem.MessageIcon(getFlagResIdFromOutPut(outPut), true))
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

    private fun transformTextIntoSpannable(text: String): SpannableString {

        val spannableString = SpannableString(text)
        spannableString.setSpan(ForegroundColorSpan(Color.YELLOW), 0, text.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        return spannableString
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

    private fun prepareAudio(audioUrl: String) {
        val uri = Uri.parse(audioUrl)
        val mediaSource = ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
        exoPlayer.playWhenReady = true
        exoPlayer.prepare(mediaSource)
    }

    private fun prepareAudio(resId: Int) {
        val rawResourceDataSource = RawResourceDataSource(context)
        rawResourceDataSource.open(DataSpec(RawResourceDataSource.buildRawResourceUri(resId)))
        val factory = DataSource.Factory { rawResourceDataSource }
        val audioSource = ExtractorMediaSource.Factory(factory).createMediaSource(rawResourceDataSource.uri)
        exoPlayer.playWhenReady = true
        exoPlayer.prepare(audioSource)

    }

    fun play(@RawRes resId: Int, audioId: Int) {
        this.audioId = audioId
        prepareAudio(resId)
    }

    fun play(outputs: List<Output>, audioId: Int) {
        this.outputs = outputs
        this.audioId = audioId
        outPutPosition = 0
        prepareAudio(outputs.first().audio)
    }


}