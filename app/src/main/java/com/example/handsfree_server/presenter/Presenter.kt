package com.example.handsfree_server.presenter


import androidx.lifecycle.MutableLiveData
import com.example.handsfree_server.CustomService
import com.example.handsfree_server.speechrecognizer.Recognizer
import com.example.handsfree_server.view.MainView


class Presenter(private val mainView: MainView) {

    lateinit var service: CustomService

    val recognizedTextLiveData by lazy { MutableLiveData<String>() }

    fun emptyRequest() = service.emptyRequest()


    fun handleReadBack(userResponse: String) {
        recognizedTextLiveData.postValue("")
        service.handleReadBack(userResponse)
        mainView.hideButtons()
        mainView.hideTopicRecyclerView()
        mainView.hideMicInput()
    }

    fun start() = service.start()


}