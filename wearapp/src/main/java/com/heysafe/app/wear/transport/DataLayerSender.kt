package com.heysafe.app.wear.transport

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class DataLayerSender(context: Context) {
    private val client = Wearable.getDataClient(context)

    suspend fun sendVitals(hr: Float, baseline: Float, motion: Float, ts: Long) {
        val req = PutDataMapRequest.create(WearMessages.PATH_VITALS).apply {
            dataMap.putFloat(WearMessages.KEY_HR, hr)
            dataMap.putFloat(WearMessages.KEY_BASELINE, baseline)
            dataMap.putFloat(WearMessages.KEY_MOTION, motion)
            dataMap.putLong(WearMessages.KEY_TS, ts)
        }
        client.putDataItem(req.asPutDataRequest().setUrgent()).await()
    }

    suspend fun sendAlertConfirmed(triggerSource: String, hrWindow: FloatArray, motionWindow: FloatArray) {
        val req = PutDataMapRequest.create(WearMessages.PATH_ALERT_CONFIRMED + "/" + System.currentTimeMillis()).apply {
            dataMap.putString(WearMessages.KEY_TRIGGER_SOURCE, triggerSource)
            dataMap.putFloatArray(WearMessages.KEY_HR_WINDOW, hrWindow)
            dataMap.putFloatArray(WearMessages.KEY_MOTION_WINDOW, motionWindow)
            dataMap.putLong(WearMessages.KEY_TS, System.currentTimeMillis())
        }
        client.putDataItem(req.asPutDataRequest().setUrgent()).await()
    }

    suspend fun sendAlertCanceled() {
        val req = PutDataMapRequest.create(WearMessages.PATH_ALERT_CANCELED + "/" + System.currentTimeMillis()).apply {
            dataMap.putLong(WearMessages.KEY_TS, System.currentTimeMillis())
        }
        client.putDataItem(req.asPutDataRequest().setUrgent()).await()
    }
}
