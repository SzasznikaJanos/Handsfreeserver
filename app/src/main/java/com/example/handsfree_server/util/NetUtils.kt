package com.example.handsfree_server.util

import android.util.Log
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CompletableDeferred

const val TAG =" HandsFree -->"
fun <T> Task<T>.asDeferred(): CompletableDeferred<T?> {
    val deferred = CompletableDeferred<T?>()

    deferred.invokeOnCompletion {
        if (deferred.isCancelled) {
            Log.e(TAG, "asDeferred: CANCELED")
            deferred.complete(null)
        }
    }

    this.addOnSuccessListener { result -> deferred.complete(result) }
    this.addOnFailureListener { exception ->
        deferred.completeExceptionally(exception)
        exception.printStackTrace()
    }

    return deferred
}