package com.mag.slam3dvideo.utils

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.Executors


class TaskRunner (resultThreadHandler: Handler? = null) {
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private val handler: Handler? = resultThreadHandler

    interface Callback<R> {
        fun onComplete(result: R)
    }
    fun <R> executeAsync(callable: Callable<R>, callback: Callback<R>) {
        executor.execute {
            val result: R = callable.call()
            if(handler== null)
                callback.onComplete(result)
            else
                handler.post { callback.onComplete(result) }
        }
    }
}