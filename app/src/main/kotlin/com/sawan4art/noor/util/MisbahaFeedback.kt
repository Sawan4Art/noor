package com.sawan4art.noor.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * اهتزاز المسبحة — تسبيحةٌ نخلة خفيفة، وإتمامُ دورةٍ نبضةٌ مزدوجة أوضح.
 * كل شيء محفوف بـ runCatching: جهاز بلا اهتزاز أو خدمة غائبة لا يكسر
 * العدّ أبداً (العدّ أبداً لا يعتمد على الاهتزاز).
 * إذن VIBRATE معرّف في المانيفست ولا حوار نظام له.
 */
class MisbahaFeedback(context: Context) {

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    /** نخلة التسبيحة الواحدة — قصيرة جداً لا تزعج مع السرعة */
    fun tap() {
        runCatching {
            vibrator?.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    /** إتمام دورة — نبضة مزدوجة مميزة: اهتزازتان بينهما فاصل */
    fun roundCompleted() {
        runCatching {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0L, 45L, 80L, 65L), -1))
        }
    }

    /** تنبيه التسرع — ثلاث نخلات متتالية أوضح: قف ما زلتَ بسرعة */
    fun rush() {
        runCatching {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0L, 70L, 90L, 70L, 90L, 70L), -1))
        }
    }
}
