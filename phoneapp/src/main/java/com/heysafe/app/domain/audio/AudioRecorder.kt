package com.heysafe.app.domain.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outFile: File? = null

    /** Starts a 30-sec-friendly recording. Returns the output file (still being written). */
    fun start(): File {
        val f = File(context.cacheDir, "alert-${System.currentTimeMillis()}.m4a")
        outFile = f
        recorder = (if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            // Mono @ 22050 Hz, 64 kbps AAC — 30-sec clip ~250 KB raw, ~330 KB base64,
            // well under Firestore's 1 MB single-doc limit.
            setAudioChannels(1)
            setAudioSamplingRate(22_050)
            setAudioEncodingBitRate(64_000)
            setOutputFile(f.absolutePath)
            prepare()
            start()
        }
        return f
    }

    /** Stops + releases the recorder. Returns the file (now fully written). */
    fun stop(): File? {
        try { recorder?.stop() } catch (_: Exception) {}
        recorder?.release()
        recorder = null
        return outFile
    }
}
