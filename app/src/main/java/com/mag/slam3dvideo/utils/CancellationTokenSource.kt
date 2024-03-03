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

    private var createdTokens: ArrayList<CancellationToken> = ArrayList<CancellationToken>()
    fun cancel() {
        synchronized(lock) {
            isCancelRequested = true
            //todo add token's callback execution
        }
    }

    override fun close() {
        createdTokens.forEach { token -> token.close() }
        createdTokens.clear()
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
//todo add Register(()->Any) method
}
