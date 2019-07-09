package com.example.handsfree_server

import android.content.Context
import android.graphics.Color
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle

import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.handsfree_server.adapters.Adapter
import com.example.handsfree_server.adapters.TopicAdapter
import com.example.handsfree_server.model.SpeechItem
import com.example.handsfree_server.view.MainView
import com.google.android.material.snackbar.Snackbar
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions


import kotlinx.android.synthetic.main.main_fragment.*


class MainFragment : Fragment(), MainView {

    private var _dialog: PopupDialog? = null

    private val adapter by lazy { Adapter() }

    private val layoutManager by lazy {
        LinearLayoutManager(context, RecyclerView.VERTICAL, false)
    }

    private val topicAdapter by lazy {
        TopicAdapter {
            viewModel.cancelRecognition()
            viewModel.handleReadBack(it)
        }
    }

    private lateinit var viewModel: MainViewModel


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.main_fragment, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProviders.of(this, MainViewModelFactory(context!!, this))[MainViewModel::class.java]
        pauseButton.setOnClickListener {
            if (pauseButton.text.toString() == "Pause") {
                pause()
            } else {
                viewModel.emptyRequest()
                pauseButton.text = "Pause"
            }
        }


        mainRecycler.layoutManager = layoutManager
        mainRecycler.adapter = adapter
        //itemAnimator
        topicsRecyclerView.adapter = topicAdapter

        viewModel.recognitionLiveData.observe(this, Observer {
            speechRecognized_textView.text = it
        })

        checkPermissions()

    }


    override fun onStop() {
        super.onStop()
        pause()
    }

    override fun pause() {
        activity!!.runOnUiThread {
            viewModel.cancelRecognition()
            viewModel.stopAudio()
            hideTopicRecyclerView()
            hideMicInput()
            pauseButton.text = "Resume"
        }
    }

    override fun showErrorMessage(errorMessage: String) {
        activity!!.runOnUiThread {
            hideTopicRecyclerView()
            hideMicInput()
            mainRecycler.visibility = View.INVISIBLE
            error_textView.visibility = View.VISIBLE
            error_textView.text = errorMessage
        }
    }

    override fun showTopicRecyclerView(topics: List<String>) {
        topicAdapter.setTopics(topics)
        topicsRecyclerView.visibility = View.VISIBLE
    }

    override fun hideTopicRecyclerView() {
        topicsRecyclerView.visibility = View.INVISIBLE
    }

    override fun hideErrorMessage() {

    }

    override fun updateQuizResult(speechItem: SpeechItem.MessageIcon?) = activity!!.runOnUiThread {
        adapter.updateResultIcon(speechItem)
    }

    override fun showButtons(buttonTexts: List<String>) = activity!!.runOnUiThread {
        /*   buttonsLayout.visibility = View.VISIBLE
           for (i in 0..4) {
               val buttonText = buttonTexts.elementAtOrNull(i)
               val button = when (i) {
                   0 -> button_one
                   1 -> button_two
                   2 -> button_three
                   else -> button_four
               }
               setButtonTextAndVisibility(button, buttonText)
           }*/
    }

    override fun showDialog(dialogType: String) {
        when (dialogType) {
            "cancelRecognition" -> createAndShowDialog("Stopped", "Start") {
                it.dismiss()
                viewModel.emptyRequest()
            }
        }
    }

    override fun hideButtons() = activity!!.runOnUiThread {
        // buttonsLayout.visibility = View.GONE
    }

    override fun hideMicInput() = activity!!.runOnUiThread {
        mic_input_imageView.visibility = View.INVISIBLE
        hideButtons()
    }

    override fun showMicInput() = activity!!.runOnUiThread {
        mic_input_imageView.visibility = View.VISIBLE
    }

    override fun addTTSBubble(speechItem: SpeechItem) = activity!!.runOnUiThread {
        adapter.addItem(speechItem)
        scrollToPositionIfNeed(false)
    }

    private fun createAndShowDialog(
            messageText: String,
            buttonText: String,
            onCancelCallBack: (dialog: PopupDialog) -> Unit
    ) {
        activity?.runOnUiThread {
            if (_dialog?.isShowing() == true) _dialog?.dismiss()
            PopupDialog().apply {
                message = messageText
                this.buttonText = buttonText
                onDismissCallBack = { onCancelCallBack(this) }
                _dialog = this
            }.show(fragmentManager, "Dialog")
        }
    }

    private fun scrollToPositionIfNeed(scrollImmediately: Boolean) {
        val positionToScroll = if (adapter.itemCount < 1) return else adapter.itemCount - 1

        try {
            if (scrollImmediately) {
                mainRecycler.smoothScrollToPosition(positionToScroll)
            } else {
                if (activity?.hasWindowFocus() == true && layoutManager.findLastVisibleItemPosition() >= positionToScroll - 1) {
                    layoutManager.scrollToPosition(positionToScroll)
                }
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    private fun checkPermissions() {
        val rationalString = "We need Voice recorder permission to use speech recognition."

        val permissionHandler = object : PermissionHandler() {
            override fun onGranted() {
                viewModel.start()
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
                context,
                x,
                rationalString,
                Permissions.Options(),
                permissionHandler
        )
    }

    private fun handleDeniedPermission(message: String) {
        val snackBar = Snackbar.make(view!!, message, Snackbar.LENGTH_INDEFINITE)
        snackBar.setActionTextColor(Color.YELLOW)
        snackBar.setAction("OK") { checkPermissions() }
        snackBar.show()
    }
}
