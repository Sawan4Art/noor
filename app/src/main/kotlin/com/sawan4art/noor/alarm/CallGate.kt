package com.sawan4art.noor.alarm

import android.content.Context
import android.media.AudioManager

/**
 * بوابة المكالمات — هل الهاتف الآن في مكالمة؟
 *
 * بلا أي إذنٍ إضافيّ (قراءة حالة الهاتف إذنٌ حساسٌ لم يطلبه المالك):
 * وضع صوت النظام يتحوّل من MODE_NORMAL أثناء مكالمات الشبكة
 * (MODE_RINGING/MODE_IN_CALL) ومكالمات الإنترنت (MODE_IN_COMMUNICATION
 * — واتساب ومثلها) وفحص المتصلين (MODE_CALL_SCREENING)، فانحرافُ
 * الوضع عن العادي دليلُ مكالمةٍ حيّة. والفحص عند لحظة دقّ التنبيه
 * نفسها وبكل نبضة غفوةٍ بعدها.
 */
object CallGate {

    fun isInCall(context: Context): Boolean {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return runCatching { audio.mode != AudioManager.MODE_NORMAL }.getOrDefault(false)
    }
}
