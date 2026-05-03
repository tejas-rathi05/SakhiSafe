package com.heysafe.app.wear.util

class RollingWindow(private val capacity: Int) {
    private val buf = ArrayDeque<Float>(capacity)
    private val lock = Any()

    fun add(v: Float) = synchronized(lock) {
        if (buf.size == capacity) buf.removeFirst()
        buf.addLast(v)
    }

    fun snapshot(): List<Float> = synchronized(lock) { buf.toList() }

    fun size(): Int = synchronized(lock) { buf.size }

    fun mean(): Float = synchronized(lock) {
        if (buf.isEmpty()) 0f else buf.sum() / buf.size
    }

    fun median(): Float = synchronized(lock) {
        if (buf.isEmpty()) return@synchronized 0f
        val s = buf.sorted()
        if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2f
    }

    fun variance(): Float = synchronized(lock) {
        if (buf.size < 2) return@synchronized 0f
        val m = if (buf.isEmpty()) 0f else buf.sum() / buf.size
        buf.sumOf { ((it - m).toDouble() * (it - m).toDouble()) }.toFloat() / buf.size
    }

    fun max(): Float = synchronized(lock) { buf.maxOrNull() ?: 0f }
    fun min(): Float = synchronized(lock) { buf.minOrNull() ?: 0f }
}
