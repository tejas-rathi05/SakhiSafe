package com.heysafe.app.data.vitals

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class VitalsSample(val hr: Float, val baseline: Float, val motion: Float, val ts: Long)

class VitalsRepository {
    private val _latest = MutableStateFlow<VitalsSample?>(null)
    val latest: StateFlow<VitalsSample?> = _latest

    private val _history = MutableStateFlow<List<VitalsSample>>(emptyList())
    val history: StateFlow<List<VitalsSample>> = _history

    fun update(s: VitalsSample) {
        _latest.value = s
        _history.value = (_history.value + s).takeLast(60) // last 60 samples for ECG
    }
}
