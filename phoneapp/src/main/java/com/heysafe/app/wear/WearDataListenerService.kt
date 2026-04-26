package com.heysafe.app.wear

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.heysafe.app.data.vitals.VitalsSample
import com.heysafe.app.di.ServiceLocator

class WearDataListenerService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val path = event.dataItem.uri.path ?: continue
            val map = DataMapItem.fromDataItem(event.dataItem).dataMap
            when {
                path == WearMessages.PATH_VITALS -> {
                    ServiceLocator.vitalsRepository.update(
                        VitalsSample(
                            hr = map.getFloat(WearMessages.KEY_HR),
                            baseline = map.getFloat(WearMessages.KEY_BASELINE),
                            motion = map.getFloat(WearMessages.KEY_MOTION),
                            ts = map.getLong(WearMessages.KEY_TS),
                        )
                    )
                }
                path.startsWith(WearMessages.PATH_ALERT_CONFIRMED) -> {
                    val src = map.getString(WearMessages.KEY_TRIGGER_SOURCE) ?: "unknown"
                    Log.i("HeySafe", "Wear alert received: source=$src — orchestrator wiring lands in Phase 4")
                    // TODO(P4.7): forward to ServiceLocator.alertOrchestrator.onWearAlert(...)
                }
                path.startsWith(WearMessages.PATH_ALERT_CANCELED) -> {
                    Log.i("HeySafe", "Wear alert canceled by user")
                }
            }
        }
    }
}
