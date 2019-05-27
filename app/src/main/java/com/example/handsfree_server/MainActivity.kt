package com.example.handsfree_server

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.handsfree_server.adapters.SpeechAdapter
import com.example.handsfree_server.api.HandsfreeClient
import com.example.handsfree_server.model.AudioPlayer

import com.example.handsfree_server.model.SpeechItem
import com.example.handsfree_server.presenter.MainPresenter
import com.example.handsfree_server.view.MainView
import com.google.android.material.snackbar.Snackbar
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*


import java.util.ArrayList
import kotlin.coroutines.CoroutineContext


class MainActivity : AppCompatActivity(), CoroutineScope, MainView {
    override fun updateView(text: String) {
        val lastSentSpeechItemPosition = adapter.lastSpeechPosition()

        if (isReadyToUpdate(lastSentSpeechItemPosition)) {
            val holder =
                mainRecycler.findViewHolderForAdapterPosition(lastSentSpeechItemPosition) as SpeechAdapter.SpeechViewHolder?

            holder?.let {
                updateViewHolderText(it, text)
            }
        }
    }

    override fun updateQuizResultImage(isCorrect: Boolean) {
        runOnUiThread {
            adapter.updateQuizResponseImage(isCorrect)
        }
    }

    override fun silentUpdateLastSpeechItem(speechResponseAsText: String) {
        adapter.updateLastSpeechItem(speechResponseAsText)
    }

    private fun scrollToPositionIfNeed(scrollImmediately: Boolean) {
        val positionToScroll = if (adapter.itemCount < 1) return else adapter.itemCount - 1

        try {
            if (scrollImmediately) {
                mainRecycler.smoothScrollToPosition(positionToScroll)
            } else {
                if (hasWindowFocus() && layoutManager.findLastVisibleItemPosition() >= positionToScroll - 1) {
                    mainRecycler.scrollToPosition(positionToScroll)
                }
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    private fun updateViewHolderText(holder: SpeechAdapter.SpeechViewHolder, userResponse: String) {
        runOnUiThread {
            holder.speechTextView.let {
                it.text = userResponse
                it.visibility = View.VISIBLE
            }
        }
    }

    private val layoutManager by lazy { LinearLayoutManager(this) }


    private fun isReadyToUpdate(lastSentSpeechItemPosition: Int): Boolean {
        val isPositionMatching = adapter.itemCount - 1 == lastSentSpeechItemPosition
        val isComputingLayout = mainRecycler.isComputingLayout
        val bubbleIsReady = adapter.validateSpeechBubbleUpdatePosition(lastSentSpeechItemPosition)
        return isPositionMatching && !isComputingLayout && bubbleIsReady
    }

    override fun addSpeechBubble(speechItem: SpeechItem) {
        runOnUiThread {
            adapter.addItem(speechItem)
            scrollToPositionIfNeed(false)
        }
    }

    override fun showMicInput() {
        mic_input_imageView.visibility = View.VISIBLE
    }

    override fun hideMicInput() {
        mic_input_imageView.visibility = View.INVISIBLE
    }


    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main


    val client = HandsfreeClient.client

    val presenter by lazy { MainPresenter(AudioPlayer(this), this) }

    private val adapter by lazy { SpeechAdapter() }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainRecycler.layoutManager = layoutManager
        mainRecycler.adapter = adapter

        checkPermissions()
    }

    private fun checkPermissions() {
        val rationalString = "We need Voice recorder permission to use speech recognition."

        val permissionHandler = object : PermissionHandler() {
            override fun onGranted() {

                presenter.bind(applicationContext)
            }

            override fun onDenied(context: Context?, deniedPermissions: ArrayList<String>?) {
                handleDeniedPermission("We need Audio Record permission so we can use Speech Recognition")
            }

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

}
