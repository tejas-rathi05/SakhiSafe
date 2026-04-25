package com.heysafe.app.wear.util

class RollingWindow(private val capacity: Int) {
    private val buf = ArrayDeque<Float>(capacity)

    fun add(v: Float) {
        if (buf.size == capacity) buf.removeFirst()
        buf.addLast(v)
    }

    fun snapshot(): List<Float> = buf.toList()

    fun size(): Int = buf.size

    fun mean(): Float = if (buf.isEmpty()) 0f else buf.sum() / buf.size

    fun median(): Float {
        if (buf.isEmpty()) return 0f
        val s = buf.sorted()
        return if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2f
    }

    fun variance(): Float {
        if (buf.size < 2) return 0f
        val m = mean()
        return buf.sumOf { ((it - m).toDouble() * (it - m).toDouble()) }.toFloat() / buf.size
    }

    fun max(): Float = buf.maxOrNull() ?: 0f
    fun min(): Float = buf.minOrNull() ?: 0f
}
