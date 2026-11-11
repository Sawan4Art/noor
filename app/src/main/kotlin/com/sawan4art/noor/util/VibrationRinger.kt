package com.sawan4art.noor.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * **اهتزاز أثناء الرنين** — بلاغ المالك v1.71: «اضف ميزه الاهتزاز اثنار
 * الرنين لكل الاصوات الصادره من التطبيق و يكون كل صوت قابل لتفعيل
 * الميزه او ايقافها».
 *
 * فالعقد هنا: نمطٌ اهتزازيٌّ متكررٌ يبدأ مع بداية الصوت (الأذان أو
 * الإقامة أو التنبيه أو معاينتها) ويُقطع فور انتهائه — لا اهتزازاً
 * لحظةً واحدةً عند نشر الإشعار كما تفعل قنوات النظام، بل اهتزازٌ
 * مواكبٌ للصوت نفسه ما دام يرن. والنمط: نبضةٌ ٧٠٠ms وقفةٌ ٣٠٠ms
 * ثم يعود من بدايته (index=0) حتى الإلغاء.
 *
 * مفاتيح كل صوتٍ في [com.sawan4art.noor.data.AdhanPrefs]:
 * adhanVibrate وiqamahVibrate وreminderVibrate — كلٌّ مستقل.
 */
object VibrationRinger {

    /** نبضة ٧٠٠ms وقفة ٣٠٠ms تتكرر من بدايتها — إحساس رنينٍ واضح */
    private val PATTERN = longArrayOf(0, 700, 300)

    private fun vibrator(context: Context): Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    /** بدء الاهتزاز المتكرر — يوقف أي اهتزازٍ سابقٍ أولاً فلا تداخل */
    fun startLooping(context: Context) {
        runCatching {
            val v = vibrator(context) ?: return
            v.cancel()
            v.vibrate(VibrationEffect.createWaveform(PATTERN, 0))
        }
    }

    /** إيقاف الاهتزاز — مع نهاية الصوت أو إيقافه أو إطفاء الخدمة */
    fun cancel(context: Context) {
        runCatching {
            vibrator(context)?.cancel()
        }
    }
}
