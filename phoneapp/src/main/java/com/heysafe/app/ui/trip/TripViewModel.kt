package com.heysafe.app.ui.trip

import androidx.lifecycle.ViewModel
import com.heysafe.app.domain.trip.TripMonitor
import com.heysafe.app.domain.trip.TripState
import kotlinx.coroutines.flow.StateFlow

class TripViewModel(private val monitor: TripMonitor) : ViewModel() {
    val trip: StateFlow<TripState?> = monitor.trip

    fun startTrip(destinationLabel: String, etaMinutes: Int) {
        monitor.start(destinationLabel.trim().ifEmpty { "Destination" }, etaMinutes * 60_000L)
    }

    fun arrived() = monitor.arrived()
}
