package com.example.handsfree_server.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.handsfree_server.R
import com.example.handsfree_server.model.SpeechItem
import com.example.handsfree_server.model.SpeechType


class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {
    val speechItems: MutableList<SpeechItem> = mutableListOf()



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val resId =
            if (viewType == SpeechType.RECEIVED.ordinal) R.layout.speech_bubble_received else R.layout.speech_bubble_sent
        val view = LayoutInflater.from(parent.context).inflate(resId, parent, false)
        return ViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int = speechItems[position].type.ordinal

    override fun getItemCount(): Int = speechItems.size


    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(speechItems[position])

    fun addItem(speechItem: SpeechItem) {
        speechItems.add(speechItem)
        notifyItemInserted(speechItems.size - 1)
    }

    fun updateQuizResponseImage(correct: Boolean) {
        findLastSentSpeechItem()?.let {
            it.messageIcon = SpeechItem.MessageIcon(if (correct) R.drawable.logo_correct else R.drawable.logo_retry)
            notifyItemChanged(speechItems.lastIndex)
        }
    }

    private fun findLastSentSpeechItem(): SpeechItem? {
        val lastPosition = speechItems.lastIndex
        if (lastPosition < 0) return null

        val lastSentSpeechItem = speechItems.findLast { it.type == SpeechType.SENT }
        if (lastSentSpeechItem == null || speechItems.indexOf(lastSentSpeechItem) != lastPosition) {
            return null
        }
        return lastSentSpeechItem
    }

    fun lastSpeechPosition() = speechItems.indexOfLast { it.type == SpeechType.SENT }


    fun validateSpeechBubbleUpdatePosition(lastSentSpeechItemPosition: Int): Boolean {
        if (lastSentSpeechItemPosition < 1) return false
        val item = speechItems[lastSentSpeechItemPosition]
        val previousItem = speechItems[lastSentSpeechItemPosition - 1]


        return item.type == SpeechType.SENT && previousItem.type == SpeechType.RECEIVED
    }

    fun silentUpdate(speechText: String) {
        speechItems.lastOrNull { it.type == SpeechType.SENT }?.text = speechText
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val speechTextView: TextView? = itemView.findViewById(R.id.speech_textView)
        private val profileIcon: ImageView? = itemView.findViewById(R.id.profilePicture)
        private var resultImageView: ImageView? = itemView.findViewById(R.id.resultImage)
        private var bubbleFrame: FrameLayout? = itemView.findViewById(R.id.frame_bubble)

        fun bind(speechItem: SpeechItem) {

            val needsProfileIcon = needsProfilePicture(speechItem, adapterPosition)

            if (needsProfileIcon) setProfileImage(speechItem)

            profileIcon?.visibility = if (needsProfileIcon) View.VISIBLE else View.INVISIBLE
            setText(speechItem)
            setResultImage(speechItem)
        }

        private fun needsProfilePicture(item: SpeechItem, position: Int): Boolean {
            val size = speechItems.size
            if (size == 1) return true
            val previousItem = if (size > 1 && position > 0) speechItems[position - 1] else null
            return item.type == SpeechType.SENT || item.type != previousItem?.type

        }

        private fun setText(currentItem: SpeechItem) {
            if (currentItem.spannableText != null) {
                speechTextView?.text = currentItem.spannableText
                bubbleFrame?.setBackgroundResource(R.drawable.bubble_speech_received)
            } else {
                bubbleFrame?.let {
                    speechTextView?.text = currentItem.text
                    it.background = null
                }
            }
        }

        private fun setProfileImage(speechItem: SpeechItem) {
            profileIcon?.let {
                Glide.with(itemView)
                    .load(if (speechItem.type == SpeechType.SENT) R.drawable.profile_placeholder else R.drawable.app_profile_icon)
                    .apply(RequestOptions.circleCropTransform())
                    .into(profileIcon)
            }
        }

        private fun setResultImage(currentItem: SpeechItem) {
            val messageIcon = currentItem.messageIcon
            if (messageIcon?.icon != null) {
                resultImageView?.let {
                    resultImageView?.visibility = View.VISIBLE
                    val glideRequest = Glide.with(itemView).load(currentItem.messageIcon?.icon)
                    if (messageIcon.withCircleCrop) glideRequest.apply(RequestOptions().circleCrop())
                    glideRequest.into(it)

                }
            } else {
                resultImageView?.visibility = View.INVISIBLE
            }
        }

    }
}