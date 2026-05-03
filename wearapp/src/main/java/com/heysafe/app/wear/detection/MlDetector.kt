package com.heysafe.app.wear.detection

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * On-device TFLite stress classifier.
 *
 * Loads `model.tflite` + `feature_scaler.json` from `assets/` if present.
 * If either is missing or fails to load, sets [unavailable] = true and `predict()`
 * returns 0f. This lets the watch keep running with heuristic-only detection
 * during development before the WESAD-trained model is generated and dropped in.
 */
class MlDetector(context: Context) {

    val unavailable: Boolean
    private val interpreter: Interpreter?
    private val mean: FloatArray?
    private val scale: FloatArray?

    init {
        val (i, m, s, missing) = loadOrDegrade(context)
        interpreter = i
        mean = m
        scale = s
        unavailable = missing
        if (unavailable) {
            Log.w(TAG, "MlDetector unavailable — model.tflite or feature_scaler.json missing from assets/. " +
                "Heuristic-only detection will be used. Run ml/train_stub.py and drop the outputs into wearapp/src/main/assets/.")
        } else {
            Log.i(TAG, "MlDetector ready: ${mean!!.size} features")
        }
    }

    /** Returns P(stress) in [0, 1], or 0f if model is unavailable. */
    fun predict(features: FloatArray): Float {
        val itp = interpreter ?: return 0f
        val mn = mean ?: return 0f
        val sc = scale ?: return 0f
        if (features.size != mn.size) {
            Log.e(TAG, "Feature size mismatch: got ${features.size}, expected ${mn.size}")
            return 0f
        }
        val scaled = FloatArray(features.size) { (features[it] - mn[it]) / sc[it] }
        val input = ByteBuffer.allocateDirect(4 * scaled.size).order(ByteOrder.nativeOrder())
        scaled.forEach { input.putFloat(it) }
        input.rewind()
        val out = Array(1) { FloatArray(1) }
        return runCatching {
            itp.run(input, out)
            out[0][0]
        }.getOrElse {
            Log.e(TAG, "TFLite inference failed", it)
            0f
        }
    }

    companion object {
        private const val TAG = "HeySafe.MlDetector"
        private const val MODEL_ASSET = "model.tflite"
        private const val SCALER_ASSET = "feature_scaler.json"

        private data class LoadResult(
            val interpreter: Interpreter?,
            val mean: FloatArray?,
            val scale: FloatArray?,
            val missing: Boolean,
        )

        private fun loadOrDegrade(context: Context): LoadResult {
            val assetNames = runCatching { context.assets.list("")?.toList().orEmpty() }.getOrDefault(emptyList())
            if (MODEL_ASSET !in assetNames || SCALER_ASSET !in assetNames) {
                return LoadResult(null, null, null, missing = true)
            }
            return runCatching {
                val itp = Interpreter(loadModel(context, MODEL_ASSET))
                val json = context.assets.open(SCALER_ASSET).bufferedReader().use { it.readText() }
                val obj = JSONObject(json)
                val mean = obj.getJSONArray("mean").toFloatArray()
                val scale = obj.getJSONArray("scale").toFloatArray()
                LoadResult(itp, mean, scale, missing = false)
            }.getOrElse {
                Log.e(TAG, "Failed to load TFLite model or scaler", it)
                LoadResult(null, null, null, missing = true)
            }
        }

        private fun loadModel(context: Context, asset: String): MappedByteBuffer {
            val fd = context.assets.openFd(asset)
            val fis = FileInputStream(fd.fileDescriptor)
            return fis.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
        }

        private fun org.json.JSONArray.toFloatArray(): FloatArray =
            FloatArray(length()) { i -> getDouble(i).toFloat() }
    }
}
