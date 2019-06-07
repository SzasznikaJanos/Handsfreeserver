package com.example.handsfree_server.presenter


import androidx.lifecycle.MutableLiveData
import com.example.handsfree_server.CustomService
import com.example.handsfree_server.view.MainView


class Presenter(private val mainView: MainView) {

    lateinit var service: CustomService

    val recognizedTextLiveData by lazy { MutableLiveData<String>() }

    fun emptyRequest() = service.emptyRequest()

    fun startRecognizer() = service.startRecognizing()


    fun handleReadBack() = service.handleReadBack(mainView)

    fun start() = service.start()


}