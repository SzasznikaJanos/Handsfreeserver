package com.example.handsfree_server.model


import android.content.Context
import android.content.Intent

import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.handsfree_server.util.LanguageCodes


class Recognizer(val context: Context, private val listener: RecognitionListener) {

    private var speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

    init {
        speechRecognizer.setRecognitionListener(listener)
    }


    fun startListening(languageId: Int) {

        val languageCode = LanguageCodes.values().find { it.code == languageId }!!
        val recognizerIntent = Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1200)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 10000)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode.name.toLowerCase())
        speechRecognizer.startListening(recognizerIntent)
    }


    fun stopRecognition() {
        speechRecognizer.stopListening()

    }

    fun cancelRecognition() {
        speechRecognizer.cancel()
        reset()
    }

    fun reset() {
        speechRecognizer.destroy()
        speechRecognizer = null
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer.setRecognitionListener(listener)
    }
}