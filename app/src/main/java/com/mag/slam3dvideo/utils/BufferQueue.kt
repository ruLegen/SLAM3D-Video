package com.mag.slam3dvideo.utils

import java.util.UUID

class BufferQueueItem<T>(item: T?, guid: UUID = UUID.randomUUID()) {
    var value: T? = item
    var id = guid
        private set
}

class BufferQueue<T>(size: Int) {
    var capacity: Int = 0
    val canConsume: Boolean
        get() = synchronized(lock) { !producedItems.isEmpty }
    val canProduce: Boolean
        get() = synchronized(lock) { !freeItems.isEmpty }

    private var freeItems: RingBuffer<BufferQueueItem<T>>
    private var producedItems: RingBuffer<BufferQueueItem<T>>
    private var trackedItems: MutableMap<UUID, BufferQueueItem<T>>
    private val lock = Any()
    private val producerLock = Object()
    private val consumerLock = Object()

    init {
        capacity = size
        freeItems = RingBuffer(capacity)
        producedItems = RingBuffer(capacity)
        trackedItems = mutableMapOf()
    }

    fun tryGetBufferToProduce(): BufferQueueItem<T>? {
        synchronized(lock) {
            if (!canProduce && trackedItems.count() == capacity)
                return null
            if (freeItems.isEmpty) {
                val item = BufferQueueItem<T>(null)
                trackedItems.set(item.id, item)
                producedItems.enqueue(item)
                return item
            } else {
                val item =freeItems.dequeue()
                producedItems.enqueue(item!!)
                return item
            }
        }
    }

    fun getBufferToProduce(): BufferQueueItem<T>? {
        synchronized(lock) {
            if (freeItems.isEmpty && trackedItems.count() != capacity) {
                val item = BufferQueueItem<T>(null)
                trackedItems.set(item.id, item)
                producedItems.enqueue(item)
                return item
            }
        }
        synchronized(producerLock){
            if (!canProduce)
                producerLock.wait()
        }
        synchronized(lock){
            val item =  freeItems.dequeue()
            producedItems.enqueue(item!!)
            return item
        }
    }

    fun tryGetBufferToConsume(): BufferQueueItem<T>? {
        synchronized(lock) {
            if (!canConsume)
                return null
            return producedItems.dequeue();
        }
    }
    fun getBufferToConsume(): BufferQueueItem<T>? {
        synchronized(lock) {
            if (!canConsume){
                synchronized(consumerLock){
                    consumerLock.wait()
                }
            }
            return producedItems.dequeue();
        }
    }
    fun tryReleaseProducedBuffer(item: BufferQueueItem<T>): Boolean {
        synchronized(lock) {
            if (!trackedItems.containsKey(item.id))
                return false
            if (producedItems.isFull)
                return false
            producedItems.enqueue(item)
        }
        return true
    }

    fun tryReleaseConsumedBuffer(item: BufferQueueItem<T>): Boolean {
        synchronized(lock) {
            if (!trackedItems.containsKey(item.id))
                return false
            if (freeItems.isFull)
                return false
            freeItems.enqueue(item)
            synchronized(producerLock){
                producerLock.notifyAll()
            }
        }
        return true;
    }
}