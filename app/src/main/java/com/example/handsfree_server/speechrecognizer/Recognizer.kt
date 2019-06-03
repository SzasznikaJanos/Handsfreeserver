package com.example.handsfree_server.speechrecognizer

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class Recognizer(context: Context,listener: RecognitionListener) {


    private val speechRecognizer by lazy {
        SpeechRecognizer.createSpeechRecognizer(context)
    }

    init {
        speechRecognizer.setRecognitionListener(listener)
    }


    fun startListening(languageCode:String){
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
        speechRecognizer.startListening(recognizerIntent)
    }

}