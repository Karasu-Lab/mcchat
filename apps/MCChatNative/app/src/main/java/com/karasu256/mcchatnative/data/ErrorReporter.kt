package com.karasu256.mcchatnative.data

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import java.io.IOException

object ErrorReporter {
    val coroutineHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("MCChat", "Unhandled error", throwable)
        if (throwable !is IOException) throw throwable
    }
}
