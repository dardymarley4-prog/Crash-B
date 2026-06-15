package com.example.game

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class EffectsHelper(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 65)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playEngineSound(speedRatio: Float) {
        val soundType = when {
            speedRatio > 0.8f -> ToneGenerator.TONE_CDMA_PIP
            speedRatio > 0.5f -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
            speedRatio > 0.2f -> ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE
            else -> ToneGenerator.TONE_PROP_BEEP
        }
        try {
            toneGenerator?.startTone(soundType, 75)
        } catch (e: Exception) {
            // Ignore if active or failed
        }
    }

    fun playCrashSound(intensityRatio: Float) {
        try {
            // High pitch crunch beep followed by low pitch
            if (intensityRatio > 0.7f) {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 350)
            } else {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 200)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playNitroSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 150)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playHorn() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_D, 250)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playUnlockSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_0, 100)
            Thread.sleep(80)
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_5, 100)
            Thread.sleep(80)
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_9, 150)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playCheckpointSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun vibrate(durationMs: Long, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        try {
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val clampedAmp = amplitude.coerceIn(1, 255)
                    vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, clampedAmp))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
