package com.sawan4art.noor.domain.qibla

/**
 * كشف عدم اتزان البوصلة وضرورة المعايرة — v1.84 (بلاغ المالك:
 * «اضف تنبيه بضروره معايره البوصله في الهاتف عند عدم اتزان البوصله
 * في الهاتف لان البوصله تتغير في الاتجاه مع الوقت»):
 *
 * **علامتا العطل** — كلاهما يكفي وحده لإعلان العطل:
 * ١. **دقة المستشعر** — نظام أندرويد يخبرنا بحالة المغنطومتر بعد
 *    كل قراءة: غير موثوق (٠) أو منخفضة (١) يعني أن المزج المغناطيسي
 *    لا يثق في حسّاسه — أول علامات الانحراف والتخبط.
 * ٢. **شدة المجال المغناطيسي** — حسّاس سليمٌ قرب سطح الأرض يقيس
 *    نحو ٢٥-٦٥ ميكروتسلا. رقمٌ خارج النطاق المعقول يعني تشويشاً:
 *    ماجنتَ حقيبةٍ أو غطاء هاتفٍ مغناطيسيٍّ أو معدناً أو سيارةً —
 *    والاتجاه حينها يرسم ما لا يشبه الشمال.
 *
 * **الهستيرesis** — علامةٌ عابرةٌ (حركة يدٍ أو لمحة تشويش) لا تُنذر:
 * العطل يجب أن يُلاحَظ متصلاً مدة [BAD_ENTRY_MS] قبل رفع التنبيه،
 * والصلاح يجب أن يُلاحَظ متصلاً مدة [GOOD_EXIT_MS] قبل رفعه — فلا
 * وامضَ للتنبيه ولا وثباتٍ لعطلٍ زال.
 *
 * منطقٌ نقيٌّ بلا أندرويد كي يُختبر آلياً بعقودٍ صارمة.
 */
object CompassCalibration {

    /** حالة دقة المستشعر — قيم SensorManager الثابتة موثقةً نقياً */
    const val ACCURACY_UNRELIABLE = 0
    const val ACCURACY_LOW = 1
    const val ACCURACY_MEDIUM = 2
    const val ACCURACY_HIGH = 3

    /** أرضية شدة المجال المعقولة بالجهاز (ميكروتسلا) — تحتها تشويش */
    const val FIELD_MIN_UT = 18f

    /** سقف شدة المجال المعقولة (ميكروتسلا) — فوقه ماجنتٌ أو معدنٌ قريب */
    const val FIELD_MAX_UT = 90f

    /** مدة استمرار العطل قبل رفع التنبيه (مللي) */
    const val BAD_ENTRY_MS = 1500L

    /** مدة استمرار الصلاح قبل رفع التنبيه (مللي) */
    const val GOOD_EXIT_MS = 1000L

    /** ذاكرة المتابعة بين القراءات — حالةٌ صغيرةٌ تُمرَّر وتُعاد */
    data class Tracker(
        val badSinceMs: Long? = null,
        val goodSinceMs: Long? = null,
        val flagged: Boolean = false,
    )

    /**
     * هل القراءة الواحدة عاطلة؟ دقةٌ دون متوسطةٍ أو شدةُ مجالٍ خارج
     * النطاق المعقول — [normUT] قد يغيب (جهاز بلا مغنطومتر مستقلٍّ)
     * فتقرر الدقة وحدها.
     */
    fun isReadingBad(accuracy: Int, normUT: Float?): Boolean {
        val accuracyBad = accuracy == ACCURACY_UNRELIABLE || accuracy == ACCURACY_LOW
        if (accuracyBad) return true
        if (normUT != null && (normUT < FIELD_MIN_UT || normUT > FIELD_MAX_UT)) return true
        return false
    }

    /**
     * خطوة المتابعة بقراءةٍ جديدة — تُعيد الذاكرة بعد تذوُّق العطل
     * والصلاح بمهَلَّيهما (هستيرesis) وتحديث راية التنبيه.
     */
    fun step(tracker: Tracker, bad: Boolean, nowMs: Long): Tracker = when {
        bad -> {
            val since = tracker.badSinceMs ?: nowMs
            tracker.copy(
                badSinceMs = since,
                goodSinceMs = null,
                flagged = tracker.flagged || (nowMs - since >= BAD_ENTRY_MS),
            )
        }
        else -> {
            val since = tracker.goodSinceMs ?: nowMs
            val cleared = tracker.flagged && (nowMs - since >= GOOD_EXIT_MS)
            tracker.copy(
                badSinceMs = null,
                goodSinceMs = since,
                flagged = !cleared && tracker.flagged,
            )
        }
    }

    /** شدة المجال المغناطيسي من مركّبات القراءة (ميكروتسلا) */
    fun fieldNormUT(x: Float, y: Float, z: Float): Float =
        kotlin.math.sqrt(x * x + y * y + z * z)
}
