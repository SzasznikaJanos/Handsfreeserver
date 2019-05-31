package com.example.handsfree_server.view

import com.example.handsfree_server.model.SpeechItem


interface MainView {

    fun hideMicInput()
    fun showMicInput()
    fun addTTSBubble(speechItem: SpeechItem)

}