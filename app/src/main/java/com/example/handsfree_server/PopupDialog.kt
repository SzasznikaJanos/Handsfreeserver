package com.example.handsfree_server


import android.os.Bundle


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.layout_dialog.view.*


/**
 * Displays a customized DialogFragment.
 *
 */
class PopupDialog : DialogFragment() {


    /**
     * CallBack which will be called after pressing the close button in the dialog view.
     * callback param will have a reference for this DialogFragment
     */
    var onDismissCallBack: (dialog: PopupDialog) -> Unit = {}

    /**
     * The displayed message in the dialog view
     */
    var message: String = ""

    /**
     * text of button.
     */
    var buttonText: String = ""

    /**
     * If the [show] call throws exception, this flag will be set to true.
     * If the flag is set to true on the  next resume callback of Activity where [show] was called. Will be called again
     * to retry displaying the dialog.
     * */
  private  var shouldShow = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        setupDialog()
        val view = inflater.inflate(R.layout.layout_dialog, container, false)
        setupView(view)
        return view
    }

    /**
     * Configure the dialog fragment options.
     *
     */
    private fun setupDialog() {
        retainInstance = true
        isCancelable = false
        dialog.setCanceledOnTouchOutside(false)
    }

    /**
     * Set the message,texts and click listeners inside the dialog view
     * [message] will be displayed in the Message TextView
     *
     */
    private fun setupView(view: View) {
        with(view) {
           dialogButton.setOnClickListener { onDismissCallBack(this@PopupDialog) }
           messageTextView.text = message
           dialogButton.text = buttonText
        }
    }

    override fun show(manager: FragmentManager?, tag: String?) {
        try {
            manager?.beginTransaction()?.add(this, tag)?.commitAllowingStateLoss()
            shouldShow = false
        } catch (e: Exception) {
            shouldShow = true
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        if ((dialog != null) && retainInstance) dialog.setDismissMessage(null)
        super.onDestroyView()
    }

    /**
     * @return true if the dialog is visible and it is showing otherwise false.
     */
    fun isShowing() = dialog != null && dialog.isShowing && !isRemoving
}

