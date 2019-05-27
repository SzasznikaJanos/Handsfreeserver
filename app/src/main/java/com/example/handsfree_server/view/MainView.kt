package com.example.handsfree_server.view

import com.example.handsfree_server.model.SpeechItem

interface MainView {
    fun updateView(text: String)
    fun addSpeechBubble(speechItem: SpeechItem)
    fun showMicInput()
    fun hideMicInput()
    fun updateQuizResultImage(isCorrect: Boolean)
    fun silentUpdateLastSpeechItem(speechResponseAsText: String)
}