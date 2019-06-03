package com.example.handsfree_server

import android.app.Application
import android.speech.RecognitionListener
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.handsfree_server.view.MainView

class MainViewModelFactory(private val mainView: MainView,private val recognitionListener: RecognitionListener, private val application: Application)
    : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainViewModel(mainView, recognitionListener,application) as T
    }

}