package com.heysafe.app.domain.alert

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

class WhatsAppLauncher(private val context: Context) {

    /** Builds the canonical wa.me URL for an E.164 phone (with or without leading "+"). */
    fun buildUrl(phoneE164: String, message: String): String {
        val phone = phoneE164.trimStart('+')
        val text = URLEncoder.encode(message, "UTF-8")
        return "https://wa.me/$phone?text=$text"
    }

    /** Launches WhatsApp for a single contact. Returns true if intent was dispatched. */
    fun launch(phoneE164: String, message: String): Boolean = runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(buildUrl(phoneE164, message)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    /** Default SOS message body. Includes name + Google Maps coordinates link. */
    fun composeMessage(userName: String, lat: Double, lng: Double): String =
        "🚨 SOS from $userName. I need help. Live location: https://maps.google.com/?q=$lat,$lng"
}
