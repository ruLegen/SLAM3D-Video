package com.mag.slam3dvideo.utils

class CancellationTokenSource : AutoCloseable {
    val token: CancellationToken
        get() {
            synchronized(lock) {
                val token = CancellationToken(this)
                createdTokens.add(token)
                return token
            }
        }
    var isCancelRequested: Boolean = false
        private set

    private var lock = Any();
    private var createdTokens: ArrayList<CancellationToken> = ArrayList()
    private var callbacks: ArrayList<()->Unit> = ArrayList()
    fun cancel() {
        synchronized(lock) {
            isCancelRequested = true
            callbacks.forEach {
                try {
                    it.invoke()
                } finally {

                }
            }
        }
    }
    override fun close() {
        createdTokens.forEach { token -> token.close() }
        createdTokens.clear()
        callbacks.clear()
    }

    fun addCallback(callback: () -> Unit) {
        synchronized(lock){
            callbacks.add(callback)
        }
    }
}

class CancellationToken() : AutoCloseable {
    private var source: CancellationTokenSource? = null
    val isCancelRequested: Boolean
        get() {
            return source?.isCancelRequested ?: false
        }


    constructor(tokenSource: CancellationTokenSource) : this() {
        source = tokenSource
    }
    override fun close() {
        source = null
    }

    fun register(callback:()->Unit){
        source?.addCallback(callback)
    }
}
