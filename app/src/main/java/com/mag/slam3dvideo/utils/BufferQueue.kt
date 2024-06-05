package com.mag.slam3dvideo.utils

import java.io.Closeable
import java.util.UUID

enum class QueueState {
    Consumed,
    Consuming,
    Produced,
    Producing,
}

/**
 * Class representing an item in the buffer queue.
 *
 * @param item The item to be stored in the buffer.
 * @param guid The unique identifier for the item.
 * @param T The type of the item, which must be a Closeable.
 */
class BufferQueueItem<T>(item: T?, guid: UUID = UUID.randomUUID())
        where T : Closeable{
    var value: T? = item
    var id = guid
        private set
}

/**
 * Class representing a buffer queue with a fixed capacity.
 *
 * @param size The size of the buffer queue.
 * @param T The type of the items in the queue, which must be Closeable.
 */
class BufferQueue<T>(size: Int) where T : Closeable {
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
    /**
     * Gets a buffer item to produce.
     * Blocks caller thread if no free buffer
     * @return The buffer item to produce or null if no item is available.
     */
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

    /**
     * Gets a buffer item to consume.
     * Block caller thread if there is no filled buffer
     * @return The buffer item to consume or null if no item is available.
     */
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

    /**
     * Releases a consumed buffer item back to the queue.
     * Awakes thread that blocked by @see getBufferToProduce
     * @param item The buffer item to be released.
     */
    fun releaseConsumedBuffer(item: BufferQueueItem<T>) {
        synchronized(lock) {
            if (!trackedItems.containsKey(item.id))
                return
            val state = queueStates[item.id]!!
            assert(state == QueueState.Consuming) { "Cannot release consumed buffer, because state is ${state}" }
            queueStates[item.id] = QueueState.Consumed
            item.value?.close()
            item.value = null
            synchronized(producerLock) {
                producerLock.notifyAll()
            }
        }
    }

    /**
     * Releases a produced buffer item to the consumer queue.
     * Awakes thread that blocked by @see getBufferToConsume
     * @param item The buffer item to be released.
     */
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