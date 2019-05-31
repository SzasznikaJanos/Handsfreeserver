package com.example.handsfree_server

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle


import android.view.View
import androidx.lifecycle.ViewModelProviders

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.handsfree_server.adapters.Adapter

import com.example.handsfree_server.model.SpeechItem

import com.example.handsfree_server.view.MainView
import com.google.android.material.snackbar.Snackbar
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.util.*


import kotlin.coroutines.CoroutineContext


class MainActivity : AppCompatActivity(), CoroutineScope, MainView {


    private val adapter by lazy { Adapter() }

    private val layoutManager by lazy {
        LinearLayoutManager(this, RecyclerView.VERTICAL, false)
    }

    private val viewModel: MainViewModel by lazy {
        ViewModelProviders.of(
            this@MainActivity,
            MainViewModelFactory(this@MainActivity, this@MainActivity.application)
        )[MainViewModel::class.java]
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainRecycler.layoutManager = layoutManager
        mainRecycler.adapter = adapter
        checkPermissions()

        viewModel.recognizedTextLiveData.observe(this, androidx.lifecycle.Observer {
            speechRecognized_textView.text = it
        })

    }


    override fun hideMicInput() = runOnUiThread {
        mic_input_imageView.visibility = View.INVISIBLE
    }

    override fun showMicInput() = runOnUiThread {
        mic_input_imageView.visibility = View.VISIBLE
    }


    override fun addTTSBubble(speechItem: SpeechItem) = addItem(speechItem)


    private fun addItem(speechItem: SpeechItem) {
        runOnUiThread {
            adapter.addItem(speechItem)
            scrollToPositionIfNeed(false)
        }
    }


    private fun scrollToPositionIfNeed(scrollImmediately: Boolean) {
        val positionToScroll = if (adapter.itemCount < 1) return else adapter.itemCount - 1

        try {
            if (scrollImmediately) {
                mainRecycler.smoothScrollToPosition(positionToScroll)
            } else {
                if (hasWindowFocus() && layoutManager.findLastVisibleItemPosition() >= positionToScroll - 1) {
                    layoutManager.scrollToPosition(positionToScroll)
                }
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }


    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main


    private fun checkPermissions() {
        val rationalString = "We need Voice recorder permission to use speech recognition."

        val permissionHandler = object : PermissionHandler() {
            override fun onGranted() {
                viewModel.bind(this@MainActivity.application)
            }

            override fun onDenied(context: Context?, deniedPermissions: ArrayList<String>?) =
                handleDeniedPermission("We need Audio Record permission so we can use Speech Recognition")

            override fun onBlocked(context: Context?, blockedList: ArrayList<String>?): Boolean {
                handleDeniedPermission("Please grand Audio Record permissions  from settings in order to use speech recognition")
                return super.onBlocked(context, blockedList)
            }

        }

        Permissions.Options()
        val x = arrayOf(android.Manifest.permission.RECORD_AUDIO)
        Permissions.check(
            this,
            x,
            rationalString,
            Permissions.Options(),
            permissionHandler
        )
    }

    private fun handleDeniedPermission(message: String) {
        val snackBar = Snackbar.make(this.window.decorView, message, Snackbar.LENGTH_INDEFINITE)
        snackBar.setActionTextColor(Color.YELLOW)
        snackBar.setAction("OK") { checkPermissions() }
        snackBar.show()
    }

    override fun onResume() {
        super.onResume()
        scrollToPositionIfNeed(false)
    }
}
