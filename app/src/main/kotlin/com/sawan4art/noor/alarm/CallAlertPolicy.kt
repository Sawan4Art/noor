package com.sawan4art.noor.alarm

import com.sawan4art.noor.util.TimeFormat

/**
 * حاكم التنبيهات وقت المكالمات — بلاغ المالك (2026-11-15):
 *
 * «انا عايز التطبيق لا يصدر تنبيه صوتي اثناء المكالمات الهاتفيه لان اللي
 * بيحصل حاليا ببقى في مكالمه و يجي وقت الاذن او الاقامه او غيره يصدر صوت
 * يشوش على المكالمه ... لما يجي وقت النبيه سواء الاذن او غيره اثناء وجود
 * مكالمه لا يصدر صوت يكتفي فقط باشعار على الشاشه بانه حان وقت الاذن ودي
 * في حاله الاذان و الاقامه اما باقي التنبيهات يصدر رنين التنبيه بعد
 * انتهاء المكالمه ب ٥ ثواني كده اللي كأن التنبيه حصل لي غفوة»
 *
 * عقدٌ نقيٌّ بلا أندرويد كي يُختبر وحدةً — قرارٌ واحدٌ لكل تنبيه:
 *
 * - **الأذان والإقامة** [AlertClass.PRAYER]: وقتُهما مقدَّسٌ لا تأخيرَ
 *   عليه ولا تقديماً — إن دقّا أثناء مكالمة فصامتا إلى الأبد ولا
 *   يعوَّضان لاحقاً («اذا مضى وقت الاذان فهو مضى لا يؤذن مره اخرى»)
 *   ويكتفي بإشعارِ شاشةٍ صامت، وللأذان الذي يحمل إقامةً يُعرض المتبقي
 *   عليها في الإشعار نفسه.
 * - **باقي التنبيهات** [AlertClass.GENERIC] — تنبيه الصلاة القريبة وما
 *   سيأتي من نوعه: تفشل إلى «غفوة» فتنتظر انتهاء المكالمة ثم ترنّ بعد
 *   خمس ثوانٍ، وإن دخلَت مكالمةٌ أخرى قبل لحظة الرنين عادت إلى
 *   الانتظار من جديد.
 */
object CallAlertPolicy {

    /** فئة التنبيه: الصلاة المقدَّسة (أذان/إقامة) أم تنبيهٌ عامّ */
    enum class AlertClass { PRAYER, GENERIC }

    /** قرار لحظة دقّ التنبيه */
    sealed class Decision {
        /** لا مكالمة — رِنّ الآن كالعادة */
        object SoundNow : Decision()
        /** أذان/إقامة في مكالمة — إشعارُ شاشةٍ صامتٌ فقط: لا صوتاً ولا إعادةً أبداً */
        object SilentNotice : Decision()
        /** تنبيهٌ عامّ في مكالمة — غفوة: فحصٌ تالٍ بعد [RECHECK_DELAY_MILLIS] */
        data class Defer(val recheckAtMillis: Long) : Decision()
    }

    /** نتيجة نبضة الفحص «هل انتهت المكالمة؟» */
    sealed class Recheck {
        /** المكالمة باقية — افحص ثانيةً بعد [recheckAtMillis] */
        data class KeepWaiting(val recheckAtMillis: Long) : Recheck()
        /** انتهت المكالمة — رِنّ بعد [ringAtMillis] */
        data class Ring(val ringAtMillis: Long) : Recheck()
    }

    /** مهلة الغفوة بعد نهاية المكالمة — بلاغ المالك: «بعد انتهاء المكالمه ب ٥ ثواني» */
    const val RING_DELAY_MILLIS: Long = 5_000L

    /** نبضة إعادة الفحص أثناء المكالمة — بلا إذنِ حالةِ الهاتف فالفحص دوريٌّ */
    const val RECHECK_DELAY_MILLIS: Long = 4_000L

    /** قرار لحظة دقّ التنبيه */
    fun decide(alertClass: AlertClass, inCall: Boolean, nowMillis: Long): Decision = when {
        !inCall -> Decision.SoundNow
        alertClass == AlertClass.PRAYER -> Decision.SilentNotice
        else -> Decision.Defer(recheckAtMillis = nowMillis + RECHECK_DELAY_MILLIS)
    }

    /** نبضة الفحص أثناء الغفوة — المكالمة باقيةٌ أم انتهت؟ */
    fun recheck(inCall: Boolean, nowMillis: Long): Recheck =
        if (inCall) {
            Recheck.KeepWaiting(nowMillis + RECHECK_DELAY_MILLIS)
        } else {
            Recheck.Ring(nowMillis + RING_DELAY_MILLIS)
        }

    /** لحظة الرنين وصلت — إن دخلت مكالمةٌ جديدةٌ في مهلة الخمس ثوانٍ عادت الغفوة */
    fun ringDue(inCall: Boolean, nowMillis: Long): Decision =
        if (inCall) {
            Decision.Defer(recheckAtMillis = nowMillis + RECHECK_DELAY_MILLIS)
        } else {
            Decision.SoundNow
        }

    /**
     * نص المتبقي على الإقامة لإشعار الأذان الصامت — بلاغ المالك: «يعرض
     * المتبقي على الاقامه» — فارغٌ إن لم تُحمل الإقامة أو فات وقتها.
     */
    fun remainingIqamahText(iqamahAtMillis: Long, nowMillis: Long): String {
        val remainingSeconds = ((iqamahAtMillis - nowMillis) / 1000L).toInt()
        if (remainingSeconds <= 0) return ""
        return "المتبقي على الإقامة: ${TimeFormat.remainingArabic(remainingSeconds)}"
    }
}
