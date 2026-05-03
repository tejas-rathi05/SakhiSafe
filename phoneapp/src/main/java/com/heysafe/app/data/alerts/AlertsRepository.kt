package com.heysafe.app.data.alerts

interface AlertsBackend {
    suspend fun create(alert: Alert): String
    suspend fun setAudioBase64(alertId: String, base64: String)
    suspend fun resolve(alertId: String)
}

class AlertsRepository(private val backend: AlertsBackend) {
    suspend fun create(alert: Alert): Result<String> = runCatching { backend.create(alert) }
    suspend fun setAudioBase64(alertId: String, base64: String): Result<Unit> = runCatching { backend.setAudioBase64(alertId, base64) }
    suspend fun resolve(alertId: String): Result<Unit> = runCatching { backend.resolve(alertId) }
}
