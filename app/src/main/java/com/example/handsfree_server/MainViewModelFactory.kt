package com.example.handsfree_server

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.handsfree_server.view.MainView

@Suppress("UNCHECKED_CAST")
class MainViewModelFactory(private val context:Context, private val mainView: MainView) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainViewModel(context, mainView) as T
    }
}