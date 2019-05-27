package com.example.handsfree_server.model


import android.text.Spannable

enum class SpeechType {
    SENT,
    RECEIVED
}

data class SpeechItem(
        var text: String? = null,
        var type: SpeechType = SpeechType.RECEIVED,
        var messageIcon: MessageIcon? = null,
        var isShowProfilePicture: Boolean = false,
        var isProfileInitialized: Boolean = false,
        var spannableText: Spannable? = null,
        var allowDuplicate: Boolean = false) {

    constructor(spannableText: Spannable, type: SpeechType = SpeechType.RECEIVED, iconImageResId: MessageIcon? = null) : this() {
        this.spannableText = spannableText
        this.messageIcon = iconImageResId
        this.type = type
    }


    data class MessageIcon(val icon: Any? = null, var withCircleCrop: Boolean = false)
}
