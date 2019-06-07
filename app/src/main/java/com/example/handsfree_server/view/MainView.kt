package com.example.handsfree_server.view

import com.example.handsfree_server.model.SpeechItem


interface MainView {

    fun hideMicInput()
    fun showMicInput()
    fun addTTSBubble(speechItem: SpeechItem)
    fun showDialog(dialogType:String)

    fun showRecogButtons()

    fun hideButtons()

    fun updateQuizResult(speechItem: SpeechItem.MessageIcon?)

    fun showErrorMessage(errorMessage:String)

    fun hideErrorMessage()
}