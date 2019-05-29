package com.example.handsfree_server

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log


import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.handsfree_server.adapters.Adapter

import com.example.handsfree_server.api.HandsfreeClient
import com.example.handsfree_server.model.AudioPlayer2

import com.example.handsfree_server.model.SpeechItem
import com.example.handsfree_server.model.SpeechType
import com.example.handsfree_server.presenter.MainPresenter
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


    val client = HandsfreeClient.client

    val presenter by lazy { MainPresenter(AudioPlayer2(this), this, TimeZone.getDefault().id) }
    private var itemHasBeenAdded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainRecycler.layoutManager = layoutManager
        mainRecycler.adapter = adapter
        checkPermissions()
    }


    override fun hideMicInput() = runOnUiThread {
        mic_input_imageView.visibility = View.INVISIBLE
    }

    override fun showMicInput() = runOnUiThread {
        mic_input_imageView.visibility = View.VISIBLE
    }

    override fun updateQuizResultImage(isCorrect: Boolean) = runOnUiThread {
        adapter.updateQuizResponseImage(isCorrect)

    }

    override fun addTTSBubble(speechItem: SpeechItem) = addItem(speechItem)

    override fun addRecognizedSpeechBubble(speechText: String, isFinal: Boolean) {
        runOnUiThread {
            if (itemHasBeenAdded) {
                updateView(speechText)
            } else {
                itemHasBeenAdded = true
                addItem(SpeechItem(speechText, type = SpeechType.SENT))
            }
            if (isFinal) {
                itemHasBeenAdded = false
                adapter.silentUpdate(speechText)
            }
        }

    }

    private fun addItem(speechItem: SpeechItem) {
        adapter.addItem(speechItem)
        scrollToPositionIfNeed(false)
    }


    private fun updateView(text: String) {
        val lastSentSpeechItemPosition = adapter.lastSpeechPosition()


        val readyToUpdate = isReadyToUpdate(lastSentSpeechItemPosition)
        Log.d(com.example.handsfree_server.util.TAG, "updateView: isReady to update? = $readyToUpdate")
        if (readyToUpdate) {
            val holder =
                mainRecycler.findViewHolderForAdapterPosition(lastSentSpeechItemPosition) as Adapter.ViewHolder?

            holder?.let {
                updateViewHolderText(it, text)
            }
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

    private fun updateViewHolderText(holder: Adapter.ViewHolder, userResponse: String) {
        holder.speechTextView?.let {
            it.text = userResponse
            it.visibility = View.VISIBLE
        }

    }

    private val layoutManager by lazy { LinearLayoutManager(this) }

    private fun isReadyToUpdate(lastSentSpeechItemPosition: Int): Boolean {
        val isPositionMatching = adapter.itemCount - 1 == lastSentSpeechItemPosition
        val isComputingLayout = mainRecycler.isComputingLayout
        val bubbleIsReady = adapter.validateSpeechBubbleUpdatePosition(lastSentSpeechItemPosition)
        return isPositionMatching && !isComputingLayout && bubbleIsReady
    }


    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main


    private fun checkPermissions() {
        val rationalString = "We need Voice recorder permission to use speech recognition."

        val permissionHandler = object : PermissionHandler() {
            override fun onGranted() = presenter.bind(applicationContext)

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
