package com.example.handsfree_server.speechrecognizer

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class Recognizer(val context: Context, private val listener: RecognitionListener) {



companion object {
    var recognizedText: String = ""
}
    private var speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

    init {
        speechRecognizer.setRecognitionListener(listener)
    }


    fun startListening(languageCode:String){
        val recognizerIntent = Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE)

        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,500)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,10000)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,10000)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"fr-FR")
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
        speechRecognizer.startListening(recognizerIntent)
    }

    fun stop() {

    }

    fun reset(){
        speechRecognizer.destroy()
        speechRecognizer = null
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer.setRecognitionListener(listener)
    }
}