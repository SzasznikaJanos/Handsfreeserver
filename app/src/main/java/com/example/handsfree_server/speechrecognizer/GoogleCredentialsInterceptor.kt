package com.example.handsfree_server.speechrecognizer

import com.google.auth.Credentials
import io.grpc.*
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

internal class GoogleCredentialsInterceptor(private val mCredentials: Credentials) : ClientInterceptor {
	private var mCached: Metadata? = null
	private var mLastMetadata: Map<String, List<String>>? = null

	private fun toHeaders(metadata: Map<String, List<String>>?): Metadata {
		val headers = Metadata()
		if (metadata != null) {
			for (key in metadata.keys) {
				val headerKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)

				metadata[key]?.let {
					for (value in it) {
						headers.put(headerKey, value)
					}
				}

			}
		}
		return headers
	}

	override fun <ReqT, RespT> interceptCall(method: MethodDescriptor<ReqT, RespT>, callOptions: CallOptions,
	                                         next: Channel): ClientCall<ReqT, RespT> {
		return object : ClientInterceptors.CheckedForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
			@Throws(StatusException::class)
			override fun checkedStart(responseListener: Listener<RespT>, headers: Metadata) {
				val cachedSaved: Metadata?
				val uri = serviceUri(next, method)
				synchronized(this) {
					val latestMetadata = getRequestMetadata(uri)
					if (mLastMetadata == null || mLastMetadata !== latestMetadata) {
						mLastMetadata = latestMetadata
						mCached = toHeaders(mLastMetadata)
					}
					cachedSaved = mCached
				}

				cachedSaved?.let { headers.merge(it) }
				delegate().start(responseListener, headers)
			}
		}
	}

	/**
	 * Generate a JWT-specific service URI. The URI is simply an identifier with enough
	 * information for a service to know that the JWT was intended for it. The URI will
	 * commonly be verified with a simple string equality check.
	 */
	@Throws(StatusException::class)
	private fun serviceUri(channel: Channel, method: MethodDescriptor<*, *>): URI {
		val authority = channel.authority() ?: throw Status.UNAUTHENTICATED.withDescription("Channel has no authority").asException()
		// Always use HTTPS, by definition.
		val scheme = "https"
		val defaultPort = 443
		val serviceName = MethodDescriptor.extractFullServiceName(method.fullMethodName)

		val path = if (!serviceName.isNullOrEmpty()) "/$serviceName" else null
		var uri: URI
		try {
			uri = URI(scheme, authority, path, null, null)
		} catch (e: URISyntaxException) {
			throw Status.UNAUTHENTICATED.withDescription("Unable to construct service URI for auth").withCause(e).asException()
		}

		// The default port must not be present. Alternative ports should be present.
		if (uri.port == defaultPort) {
			uri = removePort(uri)
		}
		return uri
	}

	@Throws(StatusException::class)
	private fun removePort(uri: URI): URI {
		try {
			return URI(uri.scheme, uri.userInfo, uri.host, -1 /* port */, uri.path, uri.query, uri.fragment)
		} catch (e: URISyntaxException) {
			throw Status.UNAUTHENTICATED.withDescription("Unable to construct service URI after removing port").withCause(e).asException()
		}

	}

	@Throws(StatusException::class)
	private fun getRequestMetadata(uri: URI): Map<String, List<String>> {
		try {
			return mCredentials.getRequestMetadata(uri)
		} catch (e: IOException) {
			mLastMetadata = null
			mCached = null

			throw Status.UNAUTHENTICATED.withCause(e).asException()
		}

	}
}
