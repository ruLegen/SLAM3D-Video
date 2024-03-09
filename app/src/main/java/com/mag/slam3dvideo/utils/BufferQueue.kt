package com.mag.slam3dvideo.utils

import java.util.UUID

enum class QueueState {
    Consumed,
    Consuming,
    Produced,
    Producing,
}

class BufferQueueItem<T>(item: T?, guid: UUID = UUID.randomUUID()) {
    var value: T? = item
    var id = guid
        private set
}

class BufferQueue<T>(size: Int) {
    var capacity: Int = size

    private var trackedItems: MutableMap<UUID, BufferQueueItem<T>> = mutableMapOf()
    private var queueStates: MutableMap<UUID, QueueState> = mutableMapOf()
    private var producedBuffersDeque: ArrayDeque<BufferQueueItem<T>> = ArrayDeque(size)
    private val lock = Any()
    private val producerLock = Object()
    private val consumerLock = Object()

    init {
        for (i in 1.rangeTo(capacity)) {
            val emptyItem = BufferQueueItem<T>(null)
            trackedItems[emptyItem.id] = emptyItem;
            queueStates[emptyItem.id] = QueueState.Consumed
        }
    }

    fun getBufferToProduce(): BufferQueueItem<T>? {
        val getItem =
            { queueStates.firstNotNullOfOrNull { item -> item.takeIf { it.value == QueueState.Consumed } } }
        var item: Map.Entry<UUID, QueueState>?;
        synchronized(lock) {
            item = getItem()
        }
        if (item == null) {
            synchronized(producerLock) {
                producerLock.wait()
                item = getItem()
            }
        }
        synchronized(lock) {
            queueStates[item!!.key] = QueueState.Producing
            return trackedItems[item!!.key]
        }
    }

    fun getBufferToConsume(): BufferQueueItem<T>? {
        val getItem = {producedBuffersDeque.removeFirstOrNull()}
        var item: BufferQueueItem<T>?
        synchronized(lock) {
            item = getItem()
        }
        if (item == null) {
            synchronized(consumerLock) {
                consumerLock.wait()
                item = getItem()
            }
        }
        synchronized(lock) {
            queueStates[item!!.id] = QueueState.Consuming
            return  item
        }
    }

    fun releaseConsumedBuffer(item: BufferQueueItem<T>) {
        synchronized(lock) {
            if (!trackedItems.containsKey(item.id))
                return
            val state = queueStates[item.id]!!
            assert(state == QueueState.Consuming) { "Cannot release consumed buffer, because state is ${state}" }
            queueStates[item.id] = QueueState.Consumed
            synchronized(producerLock) {
                producerLock.notifyAll()
            }
        }
    }

    fun releaseProducedBuffer(item: BufferQueueItem<T>) {
        synchronized(lock) {
            if (!trackedItems.containsKey(item.id))
                return
            val state = queueStates[item.id]!!
            assert(state == QueueState.Producing) { "Cannot release produced buffer, because state is ${state}" }
            synchronized(consumerLock) {
                producedBuffersDeque.addLast(trackedItems[item.id]!!)
                queueStates[item.id] = QueueState.Produced
                consumerLock.notifyAll()
            }
        }
    }
}