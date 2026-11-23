package com.sawan4art.noor

import com.sawan4art.noor.domain.qibla.CompassCalibration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقود كشف عدم اتزان البوصلة — v1.84 (بلاغ المالك: «اضف تنبيه
 * بضروره معايره البوصله في الهاتف عند عدم اتزان البوصله في الهاتف
 * لان البوصله تتغير في الاتجاه مع الوقت»):
 *
 * ١. علامتا العطل: دقةٌ دون متوسطةٍ (٠/١) أو شدةُ مجالٍ خارج نطاق
 *    الأرض المعقول (١٨-٩٠ ميكروتسلا) — كلاهما يكفي وحده.
 * ٢. الهستيرesis: العطل المتصل ١.٥ ثانية يرفع التنبيه، والصلاح
 *    المتصل ثانيةً واحدةً يرفعه — فلا وميضَ ولا وثباتٍ لعطلٍ زال.
 * ٣. القراءة العابرة الواحدة لا ترفع التنبيه قط.
 */
class CompassCalibrationTest {

    private val t = CompassCalibration.Tracker()

    // ===== علامتا العطل =====

    @Test
    fun `الدقة غير الموثوقة أو المنخفضة عطلٌ والمتوسطة والعالية ليستا`() {
        assertTrue(CompassCalibration.isReadingBad(CompassCalibration.ACCURACY_UNRELIABLE, null))
        assertTrue(CompassCalibration.isReadingBad(CompassCalibration.ACCURACY_LOW, null))
        assertFalse(CompassCalibration.isReadingBad(CompassCalibration.ACCURACY_MEDIUM, null))
        assertFalse(CompassCalibration.isReadingBad(CompassCalibration.ACCURACY_HIGH, null))
    }

    @Test
    fun `شدة المجال خارج نطاق الأرض عطلٌ وداخلٍه صلاح`() {
        // حقل الأرض السطحي ٢٥-٦٥ ميكروتسلا تقريباً
        assertFalse(CompassCalibration.isReadingBad(CompassCalibration.ACCURACY_HIGH, 45f))
        assertFalse(CompassCalibration.isReadingBad(CompassCalibration.ACCURACY_MEDIUM, 25f))
        assertFalse(CompassCalibration.isReadingBad(CompassCalibration.ACCURACY_MEDIUM, 65f))
        // ماجنتُ حقيبةٍ أو معدنٍ قريب
        assertTrue(CompassCalibration.isReadingBad(CompassCalibration.ACCURACY_HIGH, 120f))
        // تشويشٌ يخنق الحقل
        assertTrue(CompassCalibration.isReadingBad(CompassCalibration.ACCURACY_HIGH, 8f))
        // بلا شدةٍ (جهاز بلا مغنطومتر مستقلٍّ) تقرر الدقة وحدها
        assertFalse(CompassCalibration.isReadingBad(CompassCalibration.ACCURACY_HIGH, null))
    }

    @Test
    fun `شدة المجال تُحسب من المركبات الثلاث`() {
        // مثلث ٣-٤-٥
        assertEquals(5f, CompassCalibration.fieldNormUT(3f, 4f, 0f), 1e-5f)
        assertEquals(50f, CompassCalibration.fieldNormUT(30f, 40f, 0f), 1e-4f)
    }

    // ===== الهستيرesis =====

    @Test
    fun `العطل العابر لا يرفع التنبيه — يلزم اتصاله ثانية ونصف`() {
        var tr = t
        // لمحة عطلٍ ثم صلاح فوراً — لا راية
        tr = CompassCalibration.step(tr, bad = true, nowMs = 10_000)
        assertFalse(tr.flagged)
        tr = CompassCalibration.step(tr, bad = false, nowMs = 10_300)
        assertFalse(tr.flagged)
        // عطلٌ متصل أقل من المهلّ — لا راية
        tr = t
        tr = CompassCalibration.step(tr, bad = true, nowMs = 20_000)
        tr = CompassCalibration.step(tr, bad = true, nowMs = 21_400)
        assertFalse("رفع التنبيه قبل مهله!", tr.flagged)
        // بلوغ ١.٥ ثانية متصلة يرفعه
        tr = CompassCalibration.step(tr, bad = true, nowMs = 21_500)
        assertTrue(tr.flagged)
    }

    @Test
    fun `الصلاح المتصل ثانيةً واحدة يرفع التنبيه والعابر لا`() {
        var tr = CompassCalibration.step(t, bad = true, nowMs = 0)
        tr = CompassCalibration.step(tr, bad = true, nowMs = 1_500)
        assertTrue(tr.flagged)
        // صلاحٌ عابر — تبقى الراية
        tr = CompassCalibration.step(tr, bad = false, nowMs = 1_700)
        assertTrue(tr.flagged)
        tr = CompassCalibration.step(tr, bad = true, nowMs = 2_000)
        assertTrue(tr.flagged)
        // صلاحٌ متصل بلوغ ثانيةٍ كاملة من أول لحظة صلاحٍ مستقرة
        tr = CompassCalibration.step(tr, bad = false, nowMs = 2_500)
        tr = CompassCalibration.step(tr, bad = false, nowMs = 3_500)
        assertFalse(tr.flagged)
    }

    @Test
    fun `راحة طويلة بلا عطلٍ لا ترفع الراية أبداً`() {
        var tr = t
        for (sec in 0..30) {
            tr = CompassCalibration.step(tr, bad = false, nowMs = sec * 1000L)
        }
        assertFalse(tr.flagged)
    }

    @Test
    fun `عطلٌ متقطعٌ بأخبار صلاحٍ قصيرةٍ يرفع التنبيه في النهاية`() {
        // تشويشٌ متذبذب: العطل يفوز إذا راح الصلاح العابر يقطعه —
        // دخول العطل يُعاد حسابه من أول لحظة عطلٍ جديدةٍ متصلة، لكن
        // الراية المرفوعة لا تُسقط إلا بصلاحٍ متصلٍ كامل
        var tr = t
        tr = CompassCalibration.step(tr, bad = true, nowMs = 0)
        tr = CompassCalibration.step(tr, bad = true, nowMs = 700)
        tr = CompassCalibration.step(tr, bad = false, nowMs = 900) // عابر
        tr = CompassCalibration.step(tr, bad = true, nowMs = 1_100)
        tr = CompassCalibration.step(tr, bad = true, nowMs = 2_000)
        tr = CompassCalibration.step(tr, bad = true, nowMs = 2_700)
        assertTrue(tr.flagged)
    }
}
