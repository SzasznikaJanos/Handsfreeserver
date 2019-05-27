package com.example.handsfree_server.speechrecognizer

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.handsfree_server.util.TAG
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1p1beta1.*
import com.google.protobuf.ByteString

import io.grpc.ManagedChannel
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext



class SpeechService :Service(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    companion object {
        fun from(binder: IBinder, speechListener: SpeechRecognizer.SpeechListener): SpeechService {
            return (binder as SpeechBinder).service.apply {
                this.speechListener = speechListener
            }
        }
        val LOCK = Any()
    }


    private var googleCredentials: GoogleCredentials? = null

    private var mApi: SpeechGrpc.SpeechStub? = null

    private var mRequestObserver: StreamObserver<StreamingRecognizeRequest>? = null

    private lateinit var mResponseObserver: StreamObserver<StreamingRecognizeResponse>

    private var hasRetriedTheConnection = false

    var speechListener: SpeechRecognizer.SpeechListener? = null
        set(value) {
            mResponseObserver = CloudSpeechUtils.buildResponseObserver(value)
        }

    private inner class SpeechBinder : Binder() {
        internal val service: SpeechService
            get() = this@SpeechService
    }


    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Creating SpeechService")
        launch(Dispatchers.Main) {

            coroutineScope {
                Log.d(TAG, "onCreate: Inside CoroutineScope Launch")
                googleCredentials = CloudSpeechUtils.createGoogleCredentials()
                Log.d(TAG, "onCreate: credential expiring  ${googleCredentials?.accessToken?.expirationTime}")
                googleCredentials?.let {
                    Log.d(TAG, "onCreate: Creating API")
                    withContext(Dispatchers.IO) {
                        mApi = CloudSpeechUtils.createSpeechGrpcApi(it)
                    }
                    Log.d(TAG, "onCreate: API = $mApi")
                }

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Release the gRPC channel.
        if (mApi != null) {
            val channel = mApi?.channel as ManagedChannel?
            if (channel?.isShutdown == false) {
                try {
                    channel.shutdown().awaitTermination(3, TimeUnit.SECONDS)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            mApi = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = SpeechBinder()

    fun startRecognitionRequest(sampleRate: Int, languageTag: String, isSingleUtterance: Boolean, hints:List<String>? = null) {

        if (mApi == null) {
            Log.e(TAG, "API is not ready(null). Ignoring the request.")
            return
        }

        val recognitionConfig = CloudSpeechUtils.buildRecognitionConfig(sampleRate, languageTag)


        val speechHelper = CloudSpeechUtils.buildSpeechHelper(hints)

        speechHelper?.let { recognitionConfig.addSpeechContexts(speechHelper) }
        hasRetriedTheConnection = false
        try {
            requestObserverStartStreaming(recognitionConfig, isSingleUtterance)
        } catch (exception: Exception) {
            exception.printStackTrace()
            if (!hasRetriedTheConnection) {
                retryRequest(recognitionConfig, isSingleUtterance)
                hasRetriedTheConnection = true
            }
        }
    }


    private fun requestObserverStartStreaming(recognitionConfigBuilder: RecognitionConfig.Builder, isSingleUtterance: Boolean) {
        googleCredentials?.refreshIfExpired()

        mRequestObserver = mApi?.streamingRecognize(mResponseObserver)

        mRequestObserver?.onNext(StreamingRecognizeRequest.newBuilder().setStreamingConfig(
                StreamingRecognitionConfig.newBuilder().setConfig(recognitionConfigBuilder.build()).setInterimResults(true)
                        .setSingleUtterance(isSingleUtterance).build()).build())
    }

    private fun retryRequest(recognitionConfigBuilder: RecognitionConfig.Builder, isSingleUtterance: Boolean) {
        googleCredentials?.let {
            mApi = CloudSpeechUtils.createSpeechGrpcApi(it)
            requestObserverStartStreaming(recognitionConfigBuilder, isSingleUtterance)
        } ?: run {
            launch {
                googleCredentials = CloudSpeechUtils.createGoogleCredentials()
                mApi = CloudSpeechUtils.createSpeechGrpcApi(googleCredentials!!)
                requestObserverStartStreaming(recognitionConfigBuilder, isSingleUtterance)
            }
        }
    }

    /**
     * Recognizes the speech audio. This method should be called every time a chunk of byte buffer
     * is ready.
     *
     * @param data The audio data.
     * @param size The number of elements that are actually relevant in the `data`.
     */
    fun recognize(data: ByteArray?, size: Int) {
        synchronized(LOCK) {
            if (mRequestObserver == null) return

            // Call the streaming recognition API
            if (data != null) {
                val streamingRecognizeRequest = buildStreamingRequest(data, size)
                mRequestObserver?.onNext(streamingRecognizeRequest)
            }
        }
    }

    private fun buildStreamingRequest(data: ByteArray?, size: Int): StreamingRecognizeRequest? {
        val streamRecognizerBuilder = StreamingRecognizeRequest.newBuilder()
        val copyFrom = ByteString.copyFrom(data, 0, size)
        return streamRecognizerBuilder.setAudioContent(copyFrom).build()
    }


    /**
     * Finishes recognizing speech audio.
     */
    fun finishRecognizing() {
        synchronized(LOCK) {
            if (mRequestObserver == null) return
            mRequestObserver?.onCompleted()
            mRequestObserver = null
        }

    }
}