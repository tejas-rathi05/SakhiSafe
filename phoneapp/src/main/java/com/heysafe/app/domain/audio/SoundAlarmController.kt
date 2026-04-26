package com.heysafe.app.domain.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager

class SoundAlarmController(private val context: Context) {

    private var player: MediaPlayer? = null

    /** Toggle: starts the alarm if stopped, stops it if playing. */
    fun toggle() {
        if (player?.isPlaying == true) stop() else start()
    }

    fun start() {
        stop()
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = true
            setDataSource(context, uri)
            setOnPreparedListener { it.start() }
            prepareAsync()
        }
    }

    fun stop() {
        player?.runCatching { if (isPlaying) stop() }
        player?.release()
        player = null
    }
}
