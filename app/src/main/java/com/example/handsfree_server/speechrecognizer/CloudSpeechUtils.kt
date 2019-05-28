package com.example.handsfree_server.speechrecognizer


import android.util.Log
import com.example.handsfree_server.api.HandsfreeClient
import com.example.handsfree_server.speechrecognizer.SpeechRecognizer.Companion.recognizedText
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1p1beta1.RecognitionConfig
import com.google.cloud.speech.v1p1beta1.SpeechContext
import com.google.cloud.speech.v1p1beta1.SpeechGrpc
import com.google.cloud.speech.v1p1beta1.StreamingRecognizeResponse


import io.grpc.internal.DnsNameResolverProvider
import io.grpc.okhttp.OkHttpChannelProvider
import io.grpc.stub.StreamObserver
import java.io.ByteArrayInputStream


object CloudSpeechUtils {


    private val TAG = CloudSpeechUtils::class.java.simpleName
    private val SCOPE = listOf("https://www.googleapis.com/auth/cloud-platform")
    private const val HOSTNAME = "speech.googleapis.com"
    private const val PORT = 443

    suspend fun createGoogleCredentials(): GoogleCredentials? {


        val credentialsResponse = HandsfreeClient.client.getCredentialsAsync().await()

        if (credentialsResponse.code() != 200) {
            credentialsResponse.errorBody()?.let {
                Log.e(TAG, "createAccessTokenForGrpcApi: Failed to load credentials from server!")
                Log.e(TAG, "createGoogleCredentials: ${it.string()}} ")
            }
        } else {
            val credentials = credentialsResponse.body()?.bytes()
            return GoogleCredentials.fromStream(ByteArrayInputStream(credentials)).createScoped(SCOPE)
        }

        return null
    }

    private fun createAccessTokenForGrpcApi(googleCredentials: GoogleCredentials): AccessToken? {
        val token: AccessToken? = googleCredentials.refreshAccessToken()
        if (token != null) {
            val tokenExpirationTime = token.expirationTime
            val tokenValue = token.tokenValue
            return AccessToken(tokenValue, tokenExpirationTime)
        }
        return null
    }


    fun createSpeechGrpcApi(googleCredentials: GoogleCredentials): SpeechGrpc.SpeechStub? {
        googleCredentials.refreshIfExpired()

        val accessToken = createAccessTokenForGrpcApi(googleCredentials)
        return if (accessToken != null) {
            val channel = OkHttpChannelProvider().builderForAddress(HOSTNAME, PORT)
                    .nameResolverFactory(DnsNameResolverProvider())
                    .intercept(GoogleCredentialsInterceptor(GoogleCredentials.create(accessToken).createScoped(SCOPE)))
                    .build()

            SpeechGrpc.newStub(channel)
        } else {
            Log.e(TAG, "createSpeechGrpcApi: AccessToken is null")
            null
        }
    }


    fun buildSpeechHelper(hints: List<String>? = null): SpeechContext? {
        val speechContextBuilder = SpeechContext.newBuilder()
        if (!hints.isNullOrEmpty()) speechContextBuilder.addAllPhrases(hints)
        return speechContextBuilder.build()
    }

    fun buildResponseObserver(speechListener: SpeechRecognizer.SpeechListener?): StreamObserver<StreamingRecognizeResponse> =
            object : StreamObserver<StreamingRecognizeResponse> {
                override fun onNext(response: StreamingRecognizeResponse) {
                    synchronized(SpeechService.LOCK) {
                        SpeechResponse.fromApiResponse(response)?.let { speechListener?.onSpeechRecognized(it) }
                    }
                }

                override fun onError(t: Throwable) {
                    Log.e(TAG, "Error calling the API.", t)
                }

                override fun onCompleted() {
                    Log.d(TAG, "onCompleted Thread: " + Thread.currentThread().name)
                    if (recognizedText.isNotBlank()) {
                        speechListener?.onCompleted(recognizedText)
                        recognizedText = ""
                    }

                }
            }

    fun buildRecognitionConfig(sampleRate: Int, languageTag: String): RecognitionConfig.Builder {
        return RecognitionConfig.newBuilder()
                .setLanguageCode(languageTag)
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setSampleRateHertz(sampleRate)
    }
}