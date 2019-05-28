package com.example.handsfree_server.adapters


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout

import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

import com.example.handsfree_server.R
import com.example.handsfree_server.model.SpeechItem
import com.example.handsfree_server.model.SpeechType


class SpeechAdapter(private val speechItemList: MutableList<SpeechItem> = mutableListOf()) :
    RecyclerView.Adapter<SpeechAdapter.SpeechViewHolder>() {

    private var slideInLeftAnimation: Animation? = null
    private var slideInRightAnimation: Animation? = null
    private var alphaAnimation: Animation? = null
    //  private var speechItemClickListener: MainAdapter.SpeechItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeechViewHolder {
        val resId =
            if (viewType == SpeechType.SENT.ordinal) R.layout.speech_bubble_sent else R.layout.speech_bubble_received
        val itemView = LayoutInflater.from(parent.context).inflate(resId, parent, false)
        return SpeechViewHolder(itemView)
    }

    override fun getItemCount(): Int = speechItemList.size

    override fun getItemViewType(position: Int): Int = speechItemList[position].type.ordinal

    override fun onViewDetachedFromWindow(holder: SpeechViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.clearAnimations()
    }

    override fun onViewAttachedToWindow(holder: SpeechViewHolder) {
        super.onViewAttachedToWindow(holder)
        val speechItem = speechItemList[holder.adapterPosition]
        holder.setProfileImage(speechItem)
        holder.setResultImage(speechItem)
    }

    override fun onBindViewHolder(viewHolder: SpeechViewHolder, position: Int) {
        viewHolder.bind(speechItemList[position])

    }

    fun addItem(speechItem: SpeechItem) {
        speechItemList.add(speechItem)
        notifyItemInserted(speechItemList.size)
    }

    fun updateLastSpeechItem(text: String) {
        findLastSentSpeechItem()?.let {
            it.text = text
        }
    }

    fun updateQuizResponseImage(correct: Boolean) {
        findLastSentSpeechItem()?.let {
            it.messageIcon = SpeechItem.MessageIcon(if (correct) R.drawable.logo_correct else R.drawable.logo_retry)
            notifyItemChanged(speechItemList.lastIndex)
        }
    }

    private fun findLastSentSpeechItem(): SpeechItem? {
        val lastPosition = speechItemList.lastIndex
        if (lastPosition < 0) return null

        val lastSentSpeechItem = speechItemList.findLast { it.type == SpeechType.SENT }
        if (lastSentSpeechItem == null || speechItemList.indexOf(lastSentSpeechItem) != lastPosition) {
            return null
        }
        return lastSentSpeechItem
    }


    private fun needsProfilePicture(item: SpeechItem, position: Int): Boolean {
        val size = speechItemList.size
        if (size == 1) return true
        val previousItem = if (size > 1 && position > 0) speechItemList[position - 1] else null
        return item.type == SpeechType.SENT || item.type != previousItem?.type
    }

    fun lastSpeechPosition() = speechItemList.indexOfLast { it.type == SpeechType.SENT }

    fun validateSpeechBubbleUpdatePosition(position: Int): Boolean {
        if (position < 1) return false
        val item = speechItemList[position]
        val previousItem = speechItemList[position - 1]
        return item.type == SpeechType.SENT && previousItem.type == SpeechType.RECEIVED
    }


    inner class SpeechViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {


        var speechTextView: TextView = itemView.findViewById(R.id.speech_textView)
        var resultImageView: ImageView = itemView.findViewById(R.id.resultImage)
        var profileImage: ImageView = itemView.findViewById(R.id.profilePicture)

        private var bubbleFrame: FrameLayout? = itemView.findViewById(R.id.frame_bubble)
        //private var constraintFrame: ConstraintLayout = itemView.findViewById(R.id.speechFrame)
        fun bind(currentItem: SpeechItem?) = with(currentItem) {
            this?.let {
                if (!isProfileInitialized) {
                    val position = adapterPosition
                    val showProfile = needsProfilePicture(speechItemList[position], position)
                    isShowProfilePicture = showProfile
                    isProfileInitialized = true
                }

                profileImage.visibility = if (isShowProfilePicture) View.VISIBLE else View.INVISIBLE

                setProfileImage(it)
                //constraintFrame.visibility = View.VISIBLE
                setText(it)
                animate(it, adapterPosition)
                setResultImage(it)
            }
        }

        private fun setText(currentItem: SpeechItem) {
            if (currentItem.spannableText != null) {
                speechTextView.text = currentItem.spannableText
                bubbleFrame?.setBackgroundResource(R.drawable.bubble_speech_received)
            } else {
                bubbleFrame?.let {
                    speechTextView.text = currentItem.text
                    it.background = null
                }
            }
        }

        fun setProfileImage(speechItem: SpeechItem) {
            if (speechItem.type == SpeechType.SENT) {
                Glide.with(itemView).load(R.drawable.profile_placeholder)
                    .apply(RequestOptions.circleCropTransform())
                    .into(profileImage)
            } else {
                Glide.with(itemView).load(R.drawable.app_profile_icon)
                    .apply(RequestOptions.circleCropTransform())
                    .into(profileImage)
            }
        }

        fun setResultImage(currentItem: SpeechItem) {
            val messageIcon = currentItem.messageIcon

            if (messageIcon?.icon != null) {
                resultImageView.visibility = View.VISIBLE
                with(Glide.with(resultImageView).load(currentItem.messageIcon?.icon)) {
                    if (messageIcon.withCircleCrop) apply(RequestOptions().circleCrop())
                    into(resultImageView)
                }
            } else {
                resultImageView.visibility = View.INVISIBLE
            }
        }


        private fun animate(currentItem: SpeechItem, position: Int) {
            loadAnimationsIfNeeded()
            val slideAnimation: Animation? =
                if (currentItem.type == SpeechType.SENT) slideInRightAnimation else slideInLeftAnimation
            slideAnimation?.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {

                }

                override fun onAnimationEnd(animation: Animation) {
                    profileImage.clearAnimation()
                    // currentItem.wasAnimated = true
                }

                override fun onAnimationRepeat(animation: Animation) {

                }
            })
            if (profileImage.visibility != View.INVISIBLE) {
                if (itemCount > 0 && position == itemCount - 1  /*!currentItem.wasAnimated*/) {
                    profileImage.startAnimation(slideAnimation)
                    // constraintFrame.startAnimation(alphaAnimation)
                }
            }
        }


        private fun loadAnimationsIfNeeded() {
            if (slideInLeftAnimation == null || slideInRightAnimation == null || alphaAnimation == null) {
                slideInRightAnimation = AnimationUtils.loadAnimation(itemView.context, R.anim.slide_in_right)
                slideInLeftAnimation = AnimationUtils.loadAnimation(itemView.context, R.anim.slide_in_left)
                alphaAnimation = AnimationUtils.loadAnimation(itemView.context, R.anim.fade_in)
            }
        }

        fun clearAnimations() {
            //    constraintFrame.clearAnimation()
            profileImage.clearAnimation()
            speechTextView.clearAnimation()
        }
    }
}