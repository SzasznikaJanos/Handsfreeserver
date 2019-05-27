package com.example.handsfree_server.model

import android.content.Context
import android.graphics.Color

import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan

import com.example.handsfree_server.pojo.Output

import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player

import com.google.android.exoplayer2.source.ExtractorMediaSource

import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util


class AudioPlayer(val context: Context) {


    private val exoPlayer by lazy { ExoPlayerFactory.newSimpleInstance(context) }
    private val userAgent = Util.getUserAgent(context, "audioPlayer")
    private val dataSourceFactory = DefaultDataSourceFactory(context, userAgent, DefaultBandwidthMeter())
    private var outputs: List<Output> = mutableListOf()
    private var outPutPosition = 0


    private val audioListener = object : Player.EventListener {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> handleConsecutivePlay()
                Player.STATE_READY -> audioPlayerCallBack?.onAudioStart(buildSpeechItem())
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
            audioPlayerCallBack?.onAudioEnd()
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


    private fun buildSpeechItem(): SpeechItem {
        val output = outputs[outPutPosition]
        return if (output.type == "phrase") {
            buildSpeechItemSpannableFromQuizItem(output) ?: SpeechItem(output.text)
        } else {
            SpeechItem(output.text)
        }
    }

    private fun prepareAudio(audioUrl: String) {
        val uri = Uri.parse(audioUrl)
        val mediaSource = ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
        exoPlayer.playWhenReady = true
        exoPlayer.prepare(mediaSource)
    }

    fun play(outputs: List<Output>) {
        this.outputs = outputs
        outPutPosition = 0
        prepareAudio(outputs.first().audio)
    }


    interface AudioPlayerCallbacks {
        fun onAudioEnd()
        fun onAudioStart(speechItem: SpeechItem)
    }
}