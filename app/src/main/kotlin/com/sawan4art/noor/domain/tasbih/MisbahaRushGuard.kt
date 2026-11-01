package com.sawan4art.noor.domain.tasbih

/**
 * حارس التسرع في التسبيح — نقيّ بلا أندرويد وساعته محقون كي يُختبر آلياً.
 *
 * حكم المالك (alpha25): «عند التسرع في الضغط بسرعة تفوق نطق الجمل يصدر
 * تنبيه بعدم التسرع في التسبيح و يتوقف العداد عن العد».
 *
 * العتبة: أسرع جملة ذكر تُنطق بارتخاء في نحو الثانية — فسلسلة متواصلة
 * من نقراتٍ بمتوسط أقل من [maxAvgIntervalMs] (٧٠٠ مللي سكّة ≈ 1.4 نقرة/ثانية
 * على الأقل) تعني نقراً بآلة لا نطقاً. الالتقاط يتطلب **استمرارية**:
 * [triggerTaps] نقرات متتالية كل فواصلها دون العتبة — فتيرةُ العجلة
 * الصادقة (نقرة/ثانية أو أبطأ) لا تلمس أبداً.
 *
 * العقوبة: إيقاف العدّ [cooldownMs] (٣ ثوانٍ) مع تنبيه — والنقر أثناء
 * الإيقاف مُهمَل تماماً (لا عدّ ولا تناثر) ولا يمدّ فترة الإيقاف.
 */
class MisbahaRushGuard(
    private val maxAvgIntervalMs: Long = 700L,
    private val triggerTaps: Int = 6,
    private val cooldownMs: Long = 3000L,
) {

    enum class Verdict {
        /** ضغطة معدودة — تسبّح عادلاً */
        COUNTED,
        /** الضغط أثناء الإيقاف — مُهمَل بلا عدّ */
        IGNORED_PAUSED,
        /** انكشف التسرع — تنبيه وإيقاف يبدأ الآن */
        RUSH_DETECTED,
    }

    private val taps = ArrayDeque<Long>()

    /** نهاية فترة الإيقاف الحالية (0 = لا إيقاف) — تعرضها الشاشة للعدّ التنازلي */
    var pausedUntil: Long = 0L
        private set

    fun onTap(nowMs: Long): Verdict {
        if (nowMs < pausedUntil) return Verdict.IGNORED_PAUSED
        taps.addLast(nowMs)
        while (taps.size > triggerTaps) taps.removeFirst()
        if (taps.size >= triggerTaps) {
            var allFast = true
            var prev = taps.first()
            for (t in taps) {
                if (t - prev > maxAvgIntervalMs) {
                    allFast = false
                    break
                }
                prev = t
            }
            if (allFast) {
                pausedUntil = nowMs + cooldownMs
                taps.clear()
                return Verdict.RUSH_DETECTED
            }
        }
        return Verdict.COUNTED
    }

    fun isPaused(nowMs: Long): Boolean = nowMs < pausedUntil

    /** تطبيع الحالة — عند إعادة التعيين أو مغادرة الشاشة */
    fun reset() {
        taps.clear()
        pausedUntil = 0L
    }
}
