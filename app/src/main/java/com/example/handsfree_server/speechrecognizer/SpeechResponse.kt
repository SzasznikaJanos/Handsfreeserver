package com.example.handsfree_server.speechrecognizer

import com.google.cloud.speech.v1p1beta1.StreamingRecognizeResponse


data class SpeechResponse  constructor(val speechResponseAsText: String, val isFinal: Boolean, val stability: Float, val confidence: Float,var user_id:String? = null) {

	companion object {
		fun fromApiResponse(response: StreamingRecognizeResponse): SpeechResponse? {
			if (response.resultsCount > 0) {
				val result = response.getResults(0)
				if (result.alternativesCount > 0) {
					val alternatives = result.alternativesList
					val alternative = alternatives[0]
					val isFinal = result.isFinal
					val text = alternative.transcript
					val stability = result.stability
					val confidence = alternative.confidence
					return if (text.isNullOrEmpty()) null else SpeechResponse(text, isFinal, stability, confidence)
				}
			}
			return null
		}
	}

}