package com.heysafe.app.ui.alert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heysafe.app.domain.alert.AlertOrchestrator
import kotlinx.coroutines.launch

class AlertViewModel(private val orchestrator: AlertOrchestrator) : ViewModel() {
    val progress = orchestrator.progress
    fun resolve() { viewModelScope.launch { orchestrator.resolve() } }
    fun manualAlert() { viewModelScope.launch { orchestrator.onManualAlert() } }
    fun clear() { orchestrator.clear() }
}
