/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.handsfree_server.speechrecognizer

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.example.handsfree_server.util.TAG

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


/**
 * Continuously records audio and notifies the [VoiceRecorder.Callback] when voice (or any
 * sound) is heard.
 *
 * The recorded audio format is always [AudioFormat.ENCODING_PCM_16BIT] and
 * [AudioFormat.CHANNEL_IN_MONO]. This class will automatically pick the right sample rate
 * for the device. Use [.getSampleRate] to get the selected value.
 */
class VoiceRecorder internal constructor(private val mCallback: Callback) : CoroutineScope {


    private val LOCK by lazy { Any() }

    companion object {
        private val SAMPLE_RATE_CANDIDATES = intArrayOf(16000, 11025, 22050, 44100, 48000)
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

        private const val AMPLITUDE_THRESHOLD = 1500
        private const val SPEECH_TIMEOUT_MILLIS = 2000
        private const val MAX_SPEECH_LENGTH_MILLIS = 20 * 1000
    }

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private var mAudioRecord: AudioRecord? = null

    private var mBuffer: ByteArray? = null
    private var stop = false


    /**
     * The timestamp of the last time that voice is heard.
     */
    private var mLastVoiceHeardMillis = java.lang.Long.MAX_VALUE

    /**
     * The timestamp when the current voice is started.
     */
    private var mVoiceStartedMillis: Long = 0

    /**
     * Retrieves the sample rate currently used to record audio.
     *
     * @return The sample rate of recorded audio.
     */
    val sampleRate: Int
        get() = if (mAudioRecord != null) {
            mAudioRecord?.sampleRate ?: SAMPLE_RATE_CANDIDATES.last()
        } else 0

    interface Callback {

        /**
         * Called when the recorder starts hearing voice.
         */
        fun onVoiceStart()

        /**
         * Called when the recorder is hearing voice.
         *
         * @param data The audio data in [AudioFormat.ENCODING_PCM_16BIT].
         * @param size The size of the actual data in `data`.
         */
        fun onVoice(data: ByteArray, size: Int)

        /**
         * Called when the recorder stops hearing voice.
         */
        fun onVoiceEnd()
    }

    /**
     * Starts recording audio.
     *
     *
     *
     * The caller is responsible for calling [.stop] later.
     */
    fun start() {
        // Try to create a new recording session.
        stop = false


        if (mAudioRecord == null) {
            try {
                mAudioRecord = createAudioRecord()
            } catch (e: RuntimeException) {
                Log.e(TAG, "Cannot instantiate VoiceRecorder", e)
            }
        }

        mAudioRecord?.startRecording()
        analyzeVoice()
    }

    /**
     * Stops recording audio.
     */

    fun stop() {
        synchronized(LOCK) {
            Log.d(TAG, "stopping audio record")
            stop = true
            if (mAudioRecord != null) {
                dismiss()
                mAudioRecord?.stop()
            }

            mBuffer = null
        }
    }


    /**
     * Dismisses the currently ongoing utterance.
     */
    private fun dismiss() {
        if (mLastVoiceHeardMillis != java.lang.Long.MAX_VALUE) {
            Log.d(TAG, "dismissing the ongoing utterance.")
            mLastVoiceHeardMillis = java.lang.Long.MAX_VALUE
            mCallback.onVoiceEnd()
        }
    }

    /**
     * Creates a new [AudioRecord].
     *
     * @return A newly created [AudioRecord], or null if it cannot be created (missing
     * permissions?).
     */
    private fun createAudioRecord(): AudioRecord? {
        for (sampleRate in SAMPLE_RATE_CANDIDATES) {
            val sizeInBytes = AudioRecord.getMinBufferSize(sampleRate, CHANNEL, ENCODING)
            if (sizeInBytes == AudioRecord.ERROR_BAD_VALUE) {
                continue
            }
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate, CHANNEL, ENCODING, sizeInBytes
            )
            if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                mBuffer = ByteArray(sizeInBytes)
                return audioRecord
            } else {
                audioRecord.release()
            }
        }
        return null
    }


    private fun analyzeVoice() {
        launch {
            while (!stop) {
                synchronized(LOCK) {
                    mBuffer?.let {
                        val size = mAudioRecord?.read(it, 0, it.size) ?: 0
                        val now = System.currentTimeMillis()
                        if (isHearingVoice(mBuffer, size)) {
                            if (mLastVoiceHeardMillis == java.lang.Long.MAX_VALUE) {
                                mVoiceStartedMillis = now
                                mCallback.onVoiceStart()
                            }

                            mCallback.onVoice(it, size)
                            mLastVoiceHeardMillis = now
                            if (now - mVoiceStartedMillis > MAX_SPEECH_LENGTH_MILLIS) {
                                end()
                            }
                        } else if (mLastVoiceHeardMillis != java.lang.Long.MAX_VALUE) {
                            mCallback.onVoice(it, size)

                            if (now - mLastVoiceHeardMillis > SPEECH_TIMEOUT_MILLIS) {
                                end()
                            }
                        }
                    }
                }
            }
        }
    }


    private fun end() {
        mLastVoiceHeardMillis = java.lang.Long.MAX_VALUE
        mCallback.onVoiceEnd()
    }

    private fun isHearingVoice(buffer: ByteArray?, size: Int): Boolean {
        if (buffer == null) return false

        var i = 0
        while (i < size - 1) {
            // The buffer has LINEAR16 in little endian.
            var s = buffer[i + 1].toInt()
            if (s < 0) s *= -1
            s = s shl 8
            s += Math.abs(buffer[i].toInt())
            if (s > AMPLITUDE_THRESHOLD) {
                return true
            }
            i += 2
        }
        return false
    }


}

